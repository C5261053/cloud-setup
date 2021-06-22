package org.training.core.service;

import com.google.common.collect.Iterables;
import de.hybris.platform.cms2.enums.CmsPageStatus;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.cms2.servicelayer.data.RestrictionData;
import de.hybris.platform.cms2.servicelayer.services.impl.DefaultCMSContentPageService;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomCMSContentPageService extends DefaultCMSContentPageService {

    private static final Logger LOG = Logger.getLogger(CustomCMSContentPageService.class.getName());

    @Override
    protected ContentPageModel getPageForLabel(String label, List<CmsPageStatus> pageStatuses, boolean exactLabelMatch) throws CMSItemNotFoundException {
        List<String> labels = new ArrayList();
        labels.addAll(this.findLabelVariations(label, exactLabelMatch));
        Collection<AbstractPageModel> pages = (Collection)this.getSessionSearchRestrictionsDisabler().execute(() -> {
            return this.getCmsPageDao().findPagesByLabelsAndPageStatuses(labels, this.getSessionContentCatalogVersions(), pageStatuses);
        });
        if (!exactLabelMatch) {
            pages = this.findPagesForBestLabelMatch(pages, labels);
        }

        Collection<AbstractPageModel> result = this.getCmsRestrictionService().evaluatePages(pages, (RestrictionData)null);
        if (result.isEmpty()) {
            throw new CMSItemNotFoundException("No page with label [" + label + "] found.");
        } else {
            if (result.size() > 1) {
                LOG.info("More than one page found for label [" + label + "]. Returning default.");
            }

            return (ContentPageModel) Iterables.getLast(result);
        }
    }

}
