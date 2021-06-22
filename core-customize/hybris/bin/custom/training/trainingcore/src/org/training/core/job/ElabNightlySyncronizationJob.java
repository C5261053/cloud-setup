/**
 *
 */
package org.training.core.job;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.jalo.*;
import de.hybris.platform.catalog.model.CatalogModel;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.catalog.model.SyncItemJobModel;
import de.hybris.platform.cms2.servicelayer.services.CMSSiteService;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.site.BaseSiteService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author userwatson1
 *
 */
public class ElabNightlySyncronizationJob extends AbstractJobPerformable<CronJobModel>
{
	private static final Logger LOG = Logger.getLogger(ElabNightlySyncronizationJob.class);

	@Autowired
	private CatalogVersionService catalogVersionService;
	@Resource
	private BaseSiteService baseSiteService;

	@Resource(name = "cmsSiteService")
	private CMSSiteService cmsSiteService;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable#perform(de.hybris.platform.cronjob.model.CronJobModel
	 * )
	 */
	@Override
	public PerformResult perform(final CronJobModel cronJob)
	{

		String buCode = "powertools";
		baseSiteService.setCurrentBaseSite(buCode,false);

		CatalogModel defaultCatalog = cmsSiteService.getCurrentSite().getDefaultCatalog();
		if (defaultCatalog !=null){
			final CatalogVersionModel catalogVersion = catalogVersionService.getCatalogVersion(defaultCatalog.getId(),
					CatalogManager.OFFLINE_VERSION);
			LOG.debug(catalogVersion.getCatalog().getVersion() + " found!");

			final List<SyncItemJobModel> jobs = catalogVersion.getSynchronizations();
			catalogVersionService.setSessionCatalogVersions(Collections.singletonList(catalogVersion));

			LOG.debug(jobs.size() + " jobs found!");

			for (final SyncItemJobModel job : jobs)
			{
				LOG.debug("Job Code to Run: " + job.getCode());

				SyncItemJob catalogSyncJob = null;
				final Catalog catalog = CatalogManager.getInstance().getCatalog(defaultCatalog.getId());
				if (catalog != null)
				{
					final CatalogVersion catVersion = catalog.getCatalogVersion("Staged");
					catalogSyncJob = CatalogManager.getInstance().getSyncJobFromSource(catVersion);
				}

				if (catalogSyncJob == null)
				{
					LOG.error("Couldn't find 'SyncItemJob' for catalog [" + defaultCatalog.getId() + "]", null);
					return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.UNKNOWN);
				}
				else
				{
					final SyncItemCronJob syncJob = catalogSyncJob.newExecution();
					syncJob.setLogToDatabase(false);
					syncJob.setLogToFile(false);
					syncJob.setForceUpdate(false);

					LOG.info("Created cronjob [" + syncJob.getCode() + "] to synchronize catalog [" +defaultCatalog.getId() + "] staged to online version.");
					LOG.info("Configuring full version sync");

					catalogSyncJob.configureFullVersionSync(syncJob);

					LOG.info("Starting synchronization, this may take a while ...");

					catalogSyncJob.perform(syncJob, true);

					LOG.info("Synchronization complete for catalog"+defaultCatalog.getId());

					final CronJobResult result = modelService.get(syncJob.getResult());
					final CronJobStatus status = modelService.get(syncJob.getStatus());
					return new PerformResult(result, status);

				}


			}
			LOG.info("Current Site default Catalog  is null");
		}



		return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
	}

	/**
	 * @param catalogVersionService
	 *           the catalogVersionService to set
	 */
	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}

}
