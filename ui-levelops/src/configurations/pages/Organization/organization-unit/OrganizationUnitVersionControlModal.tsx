import React, { useEffect, useMemo, useState, useRef, useCallback } from "react";
import { Icon } from "antd";
import { get, isEqual } from "lodash";
import { useDispatch } from "react-redux";
import { useHistory, useLocation, useParams } from "react-router-dom";
import queryString from "query-string";
import { AntButton, AntButtonGroup, AntModal, AntTable } from "shared-resources/components";
import { versionType } from "configurations/configuration-types/OUTypes";
import { getOrgUnitSpecificVersion, setNewActiveVersion } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { orgUnitVersionIdState, orgUnitVersionPostDataState } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { WebRoutes } from "routes/WebRoutes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { ouVersionTableConfig } from "../Constants";
import "./OrgUnitVersion.styles.scss";
import { ProjectPathProps } from "classes/routeInterface";
interface VersionModalProps {
  orgUnitId: string | undefined;
}

const ORG_UNIT_VERSION_ID = "ORG_UNIT_VERSION_ID";

const OrganizationUnitVersionControlModal: React.FC<VersionModalProps> = ({ orgUnitId }) => {
  const location = useLocation();
  const { active_version, view_version } = queryString.parse(location.search);
  const [curVersion, setCurVersion] = useState<string | undefined>(undefined);
  const [versionsLoading, setVersionLoading] = useState<boolean>(false);
  const [showVersionModal, setShowVersionModal] = useState<boolean>(false);
  const [versionListData, setVersionList] = useState<versionType[]>([]);
  const activeVersionRef = useRef<string>("-1");
  const dispatch = useDispatch();
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();

  const orgVersionsState = useParamSelector(orgUnitVersionIdState, {
    id: orgUnitId
  });

  const orgVersionSetActiveState = useParamSelector(orgUnitVersionPostDataState, {
    id: ORG_UNIT_VERSION_ID
  });

  useEffect(() => {
    return () => {
      activeVersionRef.current = "-1";
      dispatch(genericRestAPISet({}, "organization_unit_version_control", "get", "-1"));
      dispatch(genericRestAPISet("false", "organization_unit_version_control", "list", ORG_UNIT_VERSION_ID));
    };
  }, []);

  useEffect(() => {
    if (orgVersionSetActiveState === "ok") {
      dispatch(getOrgUnitSpecificVersion(orgUnitId || ""));
      dispatch(genericRestAPISet("false", "organization_unit_version_control", "list", ORG_UNIT_VERSION_ID));
      history.push(
        WebRoutes.organization_page.edit(projectParams, orgUnitId, activeVersionRef.current, activeVersionRef.current)
      );
    }
  }, [orgVersionSetActiveState]);

  useEffect(() => {
    if (!isEqual(curVersion, active_version) && !!orgUnitId) {
      setVersionLoading(true);
      dispatch(getOrgUnitSpecificVersion(orgUnitId));
      setCurVersion(active_version as string);
    }
  }, [active_version, curVersion]);

  useEffect(() => {
    if (versionsLoading) {
      const { loading, error } = orgVersionsState;
      if (!loading && !error) {
        const records: { version: number }[] = get(orgVersionsState, ["data"], []);
        records.sort((a: { version: number }, b: { version: number }) => b?.version - a?.version);
        setVersionList(records as any);
        setVersionLoading(false);
      }
    }
  }, [orgVersionsState]);

  const handleVersionModalToggle = (value: boolean) => {
    setShowVersionModal(value);
  };

  const handleVersionViewClick = useCallback(
    (version: string) => {
      handleVersionModalToggle(false);
      history.push(WebRoutes.organization_page.edit(projectParams, orgUnitId, version, active_version as string));
    },
    [orgUnitId, active_version]
  );

  const handleVersionSetClick = useCallback(
    (version: string) => {
      activeVersionRef.current = version;
      dispatch(
        setNewActiveVersion(
          {
            active_version: version + "",
            ou_id: orgUnitId
          },
          ORG_UNIT_VERSION_ID
        )
      );
      handleVersionModalToggle(false);
    },
    [orgUnitId]
  );

  const mappedColumns = useMemo(() => {
    return ouVersionTableConfig.map((column: any) => {
      if (column.key === "id") {
        return {
          ...column,
          render: (item: any, record: any) => {
            const cactive = record?.active;
            if (cactive) {
              return <AntButton type="primary">Active</AntButton>;
            } else {
              return (
                <AntButtonGroup className="flex">
                  <AntButton
                    type="ghost"
                    className="mr-10"
                    onClick={(e: any) => handleVersionViewClick(record?.version)}>
                    View
                  </AntButton>
                  <AntButton type="ghost" onClick={(e: any) => handleVersionSetClick(record?.version)}>
                    Set as Active
                  </AntButton>
                </AntButtonGroup>
              );
            }
          }
        };
      }
      return column;
    });
  }, [active_version]);

  const renderFooter = useMemo(() => {
    return <AntButton onClick={(e: any) => handleVersionModalToggle(false)}>Cancel</AntButton>;
  }, []);

  return (
    <div className="version-modal-container">
      <p className="version_text">{`Showing Version ${view_version ?? "1"}`}</p>
      <AntButton
        className="version-modal-footer"
        onClick={(e: any) => handleVersionModalToggle(true)}
        disabled={!orgUnitId}>
        <Icon type="history" />
        Versions
      </AntButton>
      <AntModal
        className="version-modal"
        visible={showVersionModal}
        title={"Collection Versions"}
        footer={renderFooter}
        width={"48rem"}
        centered
        onCancel={(e: any) => handleVersionModalToggle(false)}>
        <AntTable
          bordered={false}
          columns={mappedColumns}
          dataSource={versionListData}
          pagination={false}
          className="ou-version-table"
        />
      </AntModal>
    </div>
  );
};

export default OrganizationUnitVersionControlModal;
