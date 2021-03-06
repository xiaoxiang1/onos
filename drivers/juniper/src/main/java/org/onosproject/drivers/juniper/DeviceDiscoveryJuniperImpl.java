/*
 * Copyright 2016 Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.juniper;


import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.juniper.JuniperUtils.REQ_IF_INFO;
import static org.onosproject.drivers.juniper.JuniperUtils.REQ_MAC_ADD_INFO;
import static org.onosproject.drivers.juniper.JuniperUtils.REQ_SYS_INFO;
import static org.onosproject.drivers.juniper.JuniperUtils.requestBuilder;
import static org.onosproject.drivers.utilities.XmlConfigParser.loadXmlString;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieve the Device information and ports via NETCONF for Juniper Router.
 * Tested with MX240 junos 14.2
 */
@Beta
public class DeviceDiscoveryJuniperImpl extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        DeviceId devId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(devId).getSession();
        String sysInfo;
        String chassis;
        try {
            sysInfo = session.get(requestBuilder(REQ_SYS_INFO));
            chassis = session.get(requestBuilder(REQ_MAC_ADD_INFO));
        } catch (IOException e) {
            log.warn("Failed to retrieve device details for {}", devId);
            return null;
        }
        log.trace("Device {} system-information {}", devId, sysInfo);
        DeviceDescription description =
                JuniperUtils.parseJuniperDescription(devId,
                                                     loadXmlString(sysInfo),
                                                     chassis);
        log.debug("Device {} description {}", devId, description);
        return description;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        DeviceId devId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(devId).getSession();
        String reply;
        try {
            reply = session.get(requestBuilder(REQ_IF_INFO));
        } catch (IOException e) {
            log.warn("Failed to retrieve ports for device {}", devId);
            return ImmutableList.of();
        }
        log.trace("Device {} interface-information {}", devId, reply);
        List<PortDescription> descriptions =
                JuniperUtils.parseJuniperPorts(loadXmlString(reply));
        log.debug("Device {} Discovered ports {}", devId, descriptions);
        return descriptions;
    }
}
