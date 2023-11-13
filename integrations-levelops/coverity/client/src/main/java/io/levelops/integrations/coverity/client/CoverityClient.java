package io.levelops.integrations.coverity.client;

import com.coverity.ws.v9.ConfigurationService;
import com.coverity.ws.v9.CovRemoteServiceException_Exception;
import com.coverity.ws.v9.DefectService;
import com.coverity.ws.v9.MergedDefectFilterSpecDataObj;
import com.coverity.ws.v9.MergedDefectsPageDataObj;
import com.coverity.ws.v9.PageSpecDataObj;
import com.coverity.ws.v9.ProjectIdDataObj;
import com.coverity.ws.v9.SnapshotFilterSpecDataObj;
import com.coverity.ws.v9.SnapshotIdDataObj;
import com.coverity.ws.v9.SnapshotInfoDataObj;
import com.coverity.ws.v9.SnapshotScopeDefectFilterSpecDataObj;
import com.coverity.ws.v9.SnapshotScopeSpecDataObj;
import com.coverity.ws.v9.StreamDataObj;
import com.coverity.ws.v9.StreamFilterSpecDataObj;
import com.coverity.ws.v9.StreamIdDataObj;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static io.levelops.integrations.coverity.utils.CoverityUtils.convertDate;

/**
 * Coverity Client class which should be used for making calls to coverity.
 */
@Log4j2
public class CoverityClient {

    private final ConfigurationService configurationService;
    private final DefectService defectService;

    /**
     * all arg constructor for {@link CoverityClient} class
     *
     * @param configurationService {@link ConfigurationService} object to be used for making soap calls
     * @param defectService        {@link DefectService} object to be used for making soap calls
     */
    @Builder
    public CoverityClient(ConfigurationService configurationService, DefectService defectService, boolean allowUnsafeSSL) {
        this.configurationService = configurationService;
        this.defectService = defectService;
        if (BooleanUtils.isTrue(allowUnsafeSSL)) {
            try {
                SSLContext sslContext = SSLContext.getInstance("ssl");
                sslContext.init(null, selfTrustManager, null );
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.warn("Failed to configure coverity client to ignore SSL certificate validation", e);
            }
        }
    }

    /**
     * Fetches the list of Streams for the coverity
     *
     * @return {@link List<StreamDataObj>} containing the list of Streams
     * @throws CoverityClientException when the client encounters an exception while making the call
     */
    public List<StreamDataObj> getStreams() throws CoverityClientException {
        try {
            return configurationService.getStreams(new StreamFilterSpecDataObj());
        } catch (CovRemoteServiceException_Exception e) {
            throw new CoverityClientException(e);
        }
    }

    /**
     * Fetches the list of corresponding Snapshots for the streams with {@code stremId}
     *
     * @param streamId id of the stream
     * @return {@link List<SnapshotIdDataObj>} containing the list of SnapshotIds
     * @throws CoverityClientException when the client encounters an exception while making the call
     */
    public List<SnapshotIdDataObj> getSnapshotsForStream(StreamIdDataObj streamId,
                                                         Date startDate,
                                                         Date endDate)
            throws CoverityClientException {
        try {
            SnapshotFilterSpecDataObj filterSpec = new SnapshotFilterSpecDataObj();
            filterSpec.setStartDate(convertDate(startDate));
            filterSpec.setEndDate(convertDate(endDate));
            return configurationService.getSnapshotsForStream(streamId, filterSpec);
        } catch (CovRemoteServiceException_Exception e) {
            throw new CoverityClientException(e);
        }
    }

    /**
     * Fetches the list of corresponding Snapshots Information for the snapshotIds with {@code snapshotId}
     *
     * @param snapshotId id of the stream
     * @return {@link List<SnapshotInfoDataObj>} containing the list of detailed description od snapshot
     * @throws CoverityClientException when the client encounters an exception while making the call
     */
    public List<SnapshotInfoDataObj> getSnapshotInformation(List<SnapshotIdDataObj> snapshotId)
            throws CoverityClientException {
        try {
            if(CollectionUtils.isEmpty(snapshotId))
                return List.of();
            return configurationService.getSnapshotInformation(snapshotId);
        } catch (CovRemoteServiceException_Exception e) {
            throw new CoverityClientException(e);
        }
    }

    /**
     * Fetches the Merged defects for the list of streamIds with {@code streamIds}
     *
     * @param streamIds     stream Ids
     * @return {@link MergedDefectsPageDataObj} containing the merged defects
     * @throws CoverityClientException when the client encounters an exception while making the call
     */
    public MergedDefectsPageDataObj getMergedDefectsForStreams(List<StreamIdDataObj> streamIds,
                                                               Date startDate,
                                                               Date endDate,
                                                               int pageSize,
                                                               int offset)
            throws CoverityClientException {
        try {
            MergedDefectFilterSpecDataObj filterSpec = new MergedDefectFilterSpecDataObj();
            SnapshotScopeSpecDataObj snapshotScope = new SnapshotScopeSpecDataObj();
            PageSpecDataObj pageSpec = new PageSpecDataObj();
            filterSpec.setLastDetectedStartDate(convertDate(startDate));
            filterSpec.setLastDetectedEndDate(convertDate(endDate));
            pageSpec.setPageSize(pageSize);
            pageSpec.setStartIndex(offset);
            return defectService.getMergedDefectsForStreams(streamIds, filterSpec, pageSpec, snapshotScope);
        } catch (CovRemoteServiceException_Exception e) {
            throw new CoverityClientException(e);
        }
    }

    /**
     * Fetches the Merged defects for the list of streamIds with {@code streamIds}
     *
     * @param projectId     Id of the project
     * @param filterSpec    filter object with start and end date
     * @param pageSpec      pagination filter
     * @param snapshotScope showSelector filter
     * @return {@link MergedDefectsPageDataObj} containing the merged defects
     * @throws CoverityClientException when the client encounters an exception while making the call
     */
    public MergedDefectsPageDataObj getMergedDefectsForSnapshotScope(ProjectIdDataObj projectId,
                                                                     SnapshotScopeDefectFilterSpecDataObj filterSpec,
                                                                     PageSpecDataObj pageSpec,
                                                                     SnapshotScopeSpecDataObj snapshotScope) throws CoverityClientException {
        try {
            return defectService.getMergedDefectsForSnapshotScope(projectId, filterSpec, pageSpec, snapshotScope);
        } catch (CovRemoteServiceException_Exception e) {
            throw new CoverityClientException(e);
        }
    }

    TrustManager[] selfTrustManager = new TrustManager[] {

            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    //do nothing
                    return null;
                }
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    //do nothing
                }
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    //do nothing
                }
            }
    };
}