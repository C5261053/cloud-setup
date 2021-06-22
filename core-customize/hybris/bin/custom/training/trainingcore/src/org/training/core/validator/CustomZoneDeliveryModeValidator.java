package org.training.core.validator;

import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.deliveryzone.model.ZoneDeliveryModeModel;
import de.hybris.platform.deliveryzone.model.ZoneDeliveryModeValueModel;
import de.hybris.platform.deliveryzone.model.ZoneModel;
import de.hybris.platform.order.ZoneDeliveryModeService;
import de.hybris.platform.order.exceptions.DeliveryModeInterceptorException;
import de.hybris.platform.order.interceptors.ZoneDeliveryModeValueValidator;
import de.hybris.platform.order.strategies.deliveryzone.ZDMVConsistencyStrategy;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;
import org.springframework.beans.factory.annotation.Required;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomZoneDeliveryModeValidator extends ZoneDeliveryModeValueValidator {

    private ZoneDeliveryModeService zoneDeliveryModeService;

    private ZDMVConsistencyStrategy zdmvConsistencyStrategy;

    @Override
    public void onValidate(final Object model, final InterceptorContext ctx) throws InterceptorException
    {
        if (model instanceof ZoneDeliveryModeValueModel)
        {
            // 1. check if zones clash
            final ZoneDeliveryModeValueModel zoneDeliveryModeValue = (ZoneDeliveryModeValueModel) model;
            final ZoneModel zone = zoneDeliveryModeValue.getZone();
            final ZoneDeliveryModeModel zoneDeliveryMode = zoneDeliveryModeValue.getDeliveryMode();
            final Collection<ZoneModel> existingZones = zoneDeliveryModeService.getZonesForZoneDeliveryMode(zoneDeliveryMode);
            final Set<ZoneModel> zones = new HashSet<ZoneModel>(existingZones);
            if (zones.add(zone))
            {
                final Map<CountryModel, Set<ZoneModel>> ambiguous = zdmvConsistencyStrategy.getAmbiguousCountriesForZones(zones);
                if (!ambiguous.isEmpty())
                {
                    throw new DeliveryModeInterceptorException(
                            "Illegal value for [" + zoneDeliveryMode.getCode() + "] with zone ["
                                    + zone.getCode() + "] - its countries [" + ambiguous.keySet() + "] would be mapped to more than one zone");
                }
            }
        }
    }

    @Required
    public void setZdmvConsistencyStrategy(final ZDMVConsistencyStrategy zdmvConsistencyStrategy)
    {
        this.zdmvConsistencyStrategy = zdmvConsistencyStrategy;
    }

    @Required
    public void setZoneDeliveryModeService(final ZoneDeliveryModeService zoneDeliveryModeService)
    {
        this.zoneDeliveryModeService = zoneDeliveryModeService;
    }

}
