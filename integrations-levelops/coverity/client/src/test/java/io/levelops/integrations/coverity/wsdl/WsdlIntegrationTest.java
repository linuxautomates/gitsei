package io.levelops.integrations.coverity.wsdl;

import com.coverity.ws.v9.ConfigurationService;
import com.coverity.ws.v9.CovRemoteServiceException_Exception;
import com.coverity.ws.v9.DefectService;
import com.coverity.ws.v9.MergedDefectsPageDataObj;
import com.coverity.ws.v9.PageSpecDataObj;
import com.coverity.ws.v9.ProjectDataObj;
import com.coverity.ws.v9.ProjectFilterSpecDataObj;
import com.coverity.ws.v9.SnapshotFilterSpecDataObj;
import com.coverity.ws.v9.SnapshotIdDataObj;
import com.coverity.ws.v9.SnapshotInfoDataObj;
import com.coverity.ws.v9.StreamDataObj;
import com.coverity.ws.v9.StreamFilterSpecDataObj;
import io.levelops.integrations.coverity.client.ClientAuthenticationHandlerWSS;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class WsdlIntegrationTest {

    private static final String COVERITY_URL = System.getenv("COVERITY_URL");
    private static final String COVERITY_USERNAME = System.getenv("COVERITY_USERNAME");
    private static final String COVERITY_API_KEY = System.getenv("COVERITY_API_KEY");

    private final ConfigurationService configurationService;
    private final DefectService defectService;

    public WsdlIntegrationTest() throws MalformedURLException {
        configurationService = createConfigurationService(COVERITY_URL, COVERITY_USERNAME, COVERITY_API_KEY);
        defectService = createDefectService(COVERITY_URL, COVERITY_USERNAME, COVERITY_API_KEY);
    }

    @Test
    public void testProjects() throws CovRemoteServiceException_Exception {
        final ProjectFilterSpecDataObj projectFilterSpecDataObj = new ProjectFilterSpecDataObj();
        final List<ProjectDataObj> projects = configurationService.getProjects(projectFilterSpecDataObj);
        for (ProjectDataObj project : projects) {
            System.out.println(project.getId().getName());
            System.out.println(project.getProjectKey());
            System.out.println(project.getStreams().stream().collect(Collectors.toList()));
        }
    }

    @Test
    public void testStreams() throws CovRemoteServiceException_Exception {
        final StreamFilterSpecDataObj streamDataObj = new StreamFilterSpecDataObj();
        final List<StreamDataObj> streams = configurationService.getStreams(streamDataObj);
        for (StreamDataObj stream : streams) {
            System.out.println(stream.getId().getName());
            System.out.println(stream.getPrimaryProjectId().getName());
            System.out.println(stream.getTriageStoreId().getName());
            System.out.println(stream.getComponentMapId().getName());
        }
    }

    @Test
    public void testSnapshot() throws CovRemoteServiceException_Exception {
        final StreamFilterSpecDataObj streamDataObj = new StreamFilterSpecDataObj();
        final List<StreamDataObj> streams = configurationService.getStreams(streamDataObj);
        final SnapshotFilterSpecDataObj snapshotDataObj = new SnapshotFilterSpecDataObj();
        final List<SnapshotIdDataObj> snapshots = configurationService.getSnapshotsForStream(streams.get(1).getId(),snapshotDataObj);
        for (SnapshotIdDataObj snapshot : snapshots) {
            List<SnapshotInfoDataObj> snapshotInformation = configurationService.getSnapshotInformation(List.of(snapshot));
            System.out.println(snapshotInformation.get(0).getSnapshotId().getId());
        }
    }

    @Test
    public void testStreamDefects() throws CovRemoteServiceException_Exception {
        final StreamFilterSpecDataObj streamDataObj = new StreamFilterSpecDataObj();
        final List<StreamDataObj> streams = configurationService.getStreams(streamDataObj);
        MergedDefectsPageDataObj mergedDefectsForStreams = defectService.getMergedDefectsForStreams(List.of(streams.get(0).getId()), null, new PageSpecDataObj() ,
                null);
        System.out.println(mergedDefectsForStreams.getTotalNumberOfRecords());
    }

    private ConfigurationService createConfigurationService(String serverAddr, String user, String password) throws MalformedURLException {
        com.coverity.ws.v9.ConfigurationServiceService dss = new com.coverity.ws.v9.ConfigurationServiceService(
                new URL("http://" + serverAddr + "/ws/v9/configurationservice?wsdl"),
                new QName("http://ws.coverity.com/v9", "ConfigurationServiceService"));
        ConfigurationService ds = dss.getConfigurationServicePort();
        // Attach an authentication handler to it
        BindingProvider bindingProvider = (BindingProvider) ds;
        bindingProvider.getBinding().setHandlerChain(
                List.of(new ClientAuthenticationHandlerWSS(user, password)));
        return ds;
    }

    private DefectService createDefectService(String serverAddr, String user, String password) throws MalformedURLException {
        com.coverity.ws.v9.DefectServiceService dss = new com.coverity.ws.v9.DefectServiceService(
                new URL("http://" + serverAddr + "/ws/v9/defectservice?wsdl"),
                new QName("http://ws.coverity.com/v9", "DefectServiceService"));
        DefectService ds = dss.getDefectServicePort();
        // Attach an authentication handler to it
        BindingProvider bindingProvider = (BindingProvider) ds;
        bindingProvider.getBinding().setHandlerChain(
                List.of(new ClientAuthenticationHandlerWSS(user, password)));
        return ds;
    }
}
