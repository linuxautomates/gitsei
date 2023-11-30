import { CascaderOptionType } from "antd/lib/cascader";
import { get, isEqual } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { genericList } from "reduxConfigs/actions/restapi";
import { getOUFiltersAction } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntTag, AntText, AntTooltip, SvgIcon } from "shared-resources/components";
import { usePrevious } from "shared-resources/hooks/usePrevious";
import { toTitleCase } from "utils/stringUtils";
import { getDynamicUsers, getPrefixLabel } from "./helper";
import { getSettingsPage } from "constants/routePaths";

interface OUInfoModalProps {
  selectedOU: CascaderOptionType;
}

const OUInfoModal: React.FC<OUInfoModalProps> = ({ selectedOU }) => {
  const managers = get(selectedOU, "managers", []);
  const tags = get(selectedOU, "tags", []);
  const defaultSection = get(selectedOU, ["default_section"], {});
  const usersIds = get(defaultSection, ["users"], []);
  const dynamicuserDefination = get(defaultSection, ["dynamic_user_definition"], []);
  const [tagsLoading, setTagsLoading] = useState<boolean>(false);
  const [tagsLabel, setTagsLabel] = useState<string[]>([]);
  const [usersLoading, setUsersLoading] = useState<boolean>(false);
  const [users, setUsers] = useState<string[]>([]);

  const tags_uuid = (tags || []).sort((a: any, b: any) => a - b)?.join("_");
  const sections = get(selectedOU, "sections", []);
  const [allIntegrationData, setAllIntegrationData] = useState<any[]>([]);
  const dispatch = useDispatch();
  const history = useHistory();
  const ouUuid = `${selectedOU?.id}_ou`;
  const prevOUId = usePrevious(selectedOU?.id);
  const OUIntegrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "custom_ou",
    method: "list",
    uuid: ouUuid
  });

  const tagsState = useParamSelector(getGenericUUIDSelector, {
    uri: "tags",
    method: "list",
    uuid: tags_uuid || 0
  });

  const usersState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_users",
    method: "list",
    uuid: "ou_defination_users" || 0
  });

  const handleEditOrgUnitRedirect = () => {
    const url = `${getSettingsPage()}/organization/create_org_unit/${selectedOU?.id}?ou_workspace_id=${
      selectedOU?.workspace_id
    }&ou_category_tab=${selectedOU?.ou_category_id}`;
    history.push(url);
  };

  const handleManageOrgUnitsRedirect = () => {
    const url = `${getSettingsPage()}/organization?ou_workspace_id=${selectedOU?.workspace_id}&ou_category_tab=${
      selectedOU?.ou_category_id
    }`;
    history.push(url);
  };
  const dynamicUsers = useMemo(() => {
    return getDynamicUsers(dynamicuserDefination);
  }, [dynamicuserDefination]);

  useEffect(() => {
    if (tags.length) {
      const data = get(tagsState, ["data", "records"], []);
      if (!data.length) {
        dispatch(genericList("tags", "list", { filter: { tag_ids: tags } }, null, tags_uuid));
        setTagsLoading(true);
      } else {
        const _tagsLabel = tags.map((tag: any) => {
          return data?.find((item: any) => item?.id === tag)?.name || tag;
        });
        setTagsLabel(_tagsLabel);
        setTagsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    if (!isEqual(selectedOU?.id, prevOUId)) {
      dispatch(genericList("org_users", "list", { page: 0, page_size: 1000 }, null, "ou_defination_users"));
      setUsersLoading(true);
      setUsers([]);
    }
  }, [selectedOU?.id, prevOUId]);

  useEffect(() => {
    const integrations = sections.reduce((acc: any, next: any) => {
      const integrationId = Object.keys(next.integrations || {})?.[0];
      const integrationData: any = Object.values(next.integrations || {})?.[0];
      const integration = {
        ...(integrationData || {}),
        id: integrationId,
        dynamic_user_definition: next?.["dynamic_user_definition"] || {},
        users: next?.["users"] || []
      };
      return [...acc, integration];
    }, []);
    const integrationFilters = get(OUIntegrationState, "data", []);
    if (!integrationFilters.length) {
      dispatch(getOUFiltersAction("custom_ou", "list", ouUuid, integrations));
    } else {
      setAllIntegrationData(integrationFilters);
    }
  }, [sections]);

  useEffect(() => {
    if (tagsLoading) {
      const loading = get(tagsState, "loading", true);
      const error = get(tagsState, "error", true);
      if (!loading && !error) {
        const data = get(tagsState, ["data", "records"], []);
        const _tagsLabel = (tags || [])?.map((tag: any) => {
          return data?.find((item: any) => item?.id === tag)?.name || tag;
        });
        setTagsLabel(_tagsLabel);
        setTagsLoading(false);
      }
    }
  }, [tagsState]);

  useEffect(() => {
    if (usersLoading) {
      const loading = get(usersState, "loading", true);
      const error = get(usersState, "error", true);
      if (!loading && !error) {
        const data = get(usersState, ["data", "records"], []);
        const usersLabel = (usersIds || [])?.map((id: string) => {
          return data?.find((item: any) => item?.id === id)?.full_name || id;
        });
        setUsers(usersLabel);
        setUsersLoading(false);
      }
    }
  }, [usersState]);

  useEffect(() => {
    const loading = get(OUIntegrationState, "loading", true);
    const error = get(OUIntegrationState, "error", true);
    if (!loading && !error) {
      const data = get(OUIntegrationState, "data", []);
      setAllIntegrationData(data);
    }
  }, [OUIntegrationState]);

  return (
    <div className="show-ou-detail">
      <AntText className="ou-items">
        <span className="left-span">COLLECTION NAME</span>{" "}
        <span className="ou-name right-span">
          <a onClick={handleEditOrgUnitRedirect} className="flex">
            {selectedOU?.name} <SvgIcon color={"#2967dd"} className="svg-icon-custom-ou-link" icon="externalLink" />
          </a>
          <a onClick={handleManageOrgUnitsRedirect} className="flex">
            Manage Collections{" "}
            <SvgIcon color={"#2967dd"} className="svg-icon-custom-ou-link" theme="filled" icon="externalLink" />
          </a>
        </span>
      </AntText>
      {selectedOU?.description && (
        <AntText className="ou-items">
          <span className="left-span">DESCRIPTION</span>{" "}
          <span className="right-span">{selectedOU.description || ""}</span>
        </AntText>
      )}
      {!!managers.length && (
        <AntText className="ou-items">
          <span className="left-span">MANAGERS</span>
          <span className="right-span">
            {managers.map((manager: any) => (
              <AntTooltip title={manager.full_name}>
                <AntTag className="tag-item">{manager.full_name}</AntTag>
              </AntTooltip>
            ))}
          </span>
        </AntText>
      )}
      {!!tagsLabel?.length && (
        <AntText className="ou-items">
          <span className="left-span">TAGS</span>
          <span className="right-span">
            {tagsLabel.map((tag: any) => (
              <AntTooltip title={tag}>
                <AntTag className="tag-item">{tag}</AntTag>
              </AntTooltip>
            ))}
          </span>
        </AntText>
      )}
      {!!users?.length && (
        <AntText className="ou-items">
          <span className="left-span">USERS</span>
          <span className="right-span">
            {users?.map((name: any) => (
              <AntTooltip title={name}>
                <AntTag className="tag-item">{name}</AntTag>
              </AntTooltip>
            ))}
          </span>
        </AntText>
      )}
      {!!dynamicUsers?.length &&
        dynamicUsers?.map((user: any) => (
          <AntText className="ou-items small-text">
            <span className="left-span">USER</span>{" "}
            <span className="right-span">
              {toTitleCase(user?.label)} {getPrefixLabel(user?.type)}
              <span className="blocked-text">
                {Array.isArray(user?.value) ? (
                  user?.value.map((v: any) => (
                    <AntTooltip title={v}>
                      <AntTag className="tag-item">{v}</AntTag>
                    </AntTooltip>
                  ))
                ) : (
                  <AntTooltip title={user?.value.$age || user?.value}>
                    <AntTag className="tag-item">{user?.value.$age || user?.value}</AntTag>
                  </AntTooltip>
                )}
              </span>
            </span>
          </AntText>
        ))}
      <div>
        {allIntegrationData?.map(integration => (
          <div>
            <AntText className="ou-items">
              <span className="left-span">INTEGRATION</span> <span className="right-span">{integration?.name}</span>
            </AntText>
            {integration.filters.map((filter: any) => (
              <AntText className="ou-items small-text">
                <span className="left-span">FILTER</span>{" "}
                <span className="right-span">
                  {toTitleCase(filter?.label)} {getPrefixLabel(filter?.type)}
                  <span className="blocked-text">
                    {Array.isArray(filter?.value) ? (
                      filter?.value.map((v: any) => {
                        if (typeof v === "string") {
                          return (
                            <AntTooltip title={v}>
                              <AntTag className="tag-item">{v}</AntTag>
                            </AntTooltip>
                          );
                        }
                        if (typeof v === "object" && v?.values) {
                          // temporary fix for demo  handling different schema, ideally OU definition should have always correct schema
                          return (v?.values || []).map((rec: string) => {
                            return (
                              <AntTooltip title={rec}>
                                <AntTag className="tag-item">{rec}</AntTag>
                              </AntTooltip>
                            );
                          });
                        }
                      })
                    ) : (
                      <AntTooltip title={filter?.value.$age || filter?.value}>
                        <AntTag className="tag-item">{filter?.value.$age || filter?.value}</AntTag>
                      </AntTooltip>
                    )}
                  </span>
                </span>
              </AntText>
            ))}
            {!!integration?.dynamic_user_definition?.length &&
              (integration?.dynamic_user_definition || []).map((user: any) => (
                <AntText className="ou-items small-text">
                  <span className="left-span">USER</span>{" "}
                  <span className="right-span">
                    {toTitleCase(user?.label)} {getPrefixLabel(user?.type)}
                    <span className="blocked-text">
                      {Array.isArray(user?.value) ? (
                        user?.value.map((v: any) => (
                          <AntTooltip title={v}>
                            <AntTag className="tag-item">{v}</AntTag>
                          </AntTooltip>
                        ))
                      ) : (
                        <AntTooltip title={user?.value.$age || user?.value}>
                          <AntTag className="tag-item">{user?.value.$age || user?.value}</AntTag>
                        </AntTooltip>
                      )}
                    </span>
                  </span>
                </AntText>
              ))}
            {!!integration?.users?.length && (
              <AntText className="ou-items">
                <span className="left-span">USERS</span>
                <span className="right-span">
                  {integration?.users?.map((name: any) => (
                    <AntTooltip title={name}>
                      <AntTag className="tag-item">{name}</AntTag>
                    </AntTooltip>
                  ))}
                </span>
              </AntText>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default OUInfoModal;
