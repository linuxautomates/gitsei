import React, { useCallback, useEffect, useState } from "react";
import { useHeader } from "../../../../custom-hooks/useHeader";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { CANCEL_ACTION_KEY, getActionButtons, SAVE_ACTION_KEY } from "../../lead-time-profiles/helpers/helpers";
import "./TrellisProfile.scss";
import { notification } from "antd";
import { useDispatch, useSelector } from "react-redux";
import { ORG_UNIT_LIST_ID } from "configurations/pages/Organization/Constants";
import {
  clearSavedTrellisProfile,
  createTrellisProfileAction,
  getTrellisProfileAction,
  trellisProfilesPartialUpdateAction,
  trellisProfilesUpdateAction
} from "reduxConfigs/actions/restapi/trellisProfileActions";
import { get } from "lodash";
import TrellisProfileSection from "./TrellisProfileSection";
import Loader from "components/Loader/Loader";
import {
  trellisProfileDetailsSelector,
  trellisProfileListSelector,
  trellisProfileSavingStatusSelector
} from "reduxConfigs/selectors/trellisProfileSelectors";
import {
  RestTrellisProfileFeatures,
  RestTrellisProfileSections,
  RestTrellisScoreProfile
} from "../../../../classes/RestTrellisProfile";
import { useTicketCategorizationFilters } from "../../../../custom-hooks";
import { Entitlement, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import BasicInfo from "./BasicInfo";
import { getBreadcrumbsForTrellisProfile } from "configurations/pages/ticket-categorization/helper/getBreadcumsForTrellisPage";
import Associations from "./Associations";
import Factors from "./Factors";
import { getBaseUrl, TRELLIS_SCORE_PROFILE_ROUTES } from "constants/routePaths";
import TrellisProfileMenu from "./TrellisProfileMenu";
import {
  TrellisDetailsState,
  TrellisProfilesListState,
  TrellisSavingState
} from "reduxConfigs/reducers/trellisProfileReducer";
import { TRELLIS_PROFILE_MENU, TRELLIS_SECTION_MAPPING } from "../constant";
import { OrganizationUnitList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { orgUnitListDataState } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { validateProfile } from "./helper";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

const TrellisProfile: React.FC = () => {
  const location = useLocation();
  const dispatch = useDispatch();
  const history = useHistory();
  const params = useParams();
  const profileId = (params as any).id;

  const { setupHeader, onActionButtonClick } = useHeader(location.pathname);
  const entDevProdEdit = useHasEntitlements(Entitlement.SETTING_DEV_PRODUCTIVITY);
  const { apiData: ticketCategorizationData, apiLoading } = useTicketCategorizationFilters("dev_profile", []);
  const [trellisProfile, setTrellisProfile] = useState<RestTrellisScoreProfile>(new RestTrellisScoreProfile(null));
  const [selectedSection, setSelectedSection] = useState<RestTrellisProfileSections>(
    new RestTrellisProfileSections(null)
  );
  const [selectedMenu, setSelectedMenu] = useState<TRELLIS_PROFILE_MENU>(TRELLIS_PROFILE_MENU.BASIC_INFO);
  const [loading, setLoading] = useState<boolean>(true);
  const [updating, setUpdating] = useState(false);

  const trellisProfileDetailsState: TrellisDetailsState = useSelector(trellisProfileDetailsSelector);

  const [profilesList, setProfilesList] = useState<Array<any>>([]);
  const trellisProfilesState: TrellisProfilesListState = useSelector(trellisProfileListSelector);
  const profileSavingState: TrellisSavingState = useSelector(trellisProfileSavingStatusSelector);
  const orgUnitListState = useParamSelector(orgUnitListDataState, { id: ORG_UNIT_LIST_ID });

  useEffect(() => {
    if (!trellisProfilesState.isLoading && !trellisProfilesState.error) {
      const records = get(trellisProfilesState, ["data", "records"], []);
      setProfilesList(records);
    }
  }, [trellisProfilesState]);

  const [createAccess, editAccess] = useConfigScreenPermissions();

  const oldReadonly = getRBACPermission(PermeableMetrics.TRELLIS_PROFILE_READ_ONLY);
  const hasSaveAccess = window.isStandaloneApp ? !oldReadonly : profileId === "new" ? createAccess : editAccess;

  useEffect(() => {
    const errorMsg = validateProfile(trellisProfile);
    setupHeader({
      action_buttons: {
        action_cancel: {
          type: "secondary",
          label: "Cancel",
          hasClicked: false,
          disabled: false,
          showProgress: false
        },
        ...getActionButtons(
          !hasSaveAccess || !entDevProdEdit || errorMsg,
          !entDevProdEdit ? TOOLTIP_ACTION_NOT_ALLOWED : errorMsg || ""
        )
      },
      showFullScreenBottomSeparator: true,
      title: trellisProfile.name || "New Profile",
      description: trellisProfile.description || "",
      bread_crumbs: getBreadcrumbsForTrellisProfile(profileId === "new" ? "New Profile" : "Edit Profile"),
      bread_crumbs_position: "before"
    });
  }, [trellisProfile, profileId, entDevProdEdit, setupHeader]);

  useEffect(() => {
    if (profileId === "new") {
      setSelectedSection(new RestTrellisProfileSections({ name: "basicInfo" }));
      setLoading(false);
    } else {
      dispatch(getTrellisProfileAction(profileId));
    }
    return () => {
      dispatch(clearSavedTrellisProfile());
    };
  }, [profileId]);

  useEffect(() => {
    const loading = get(orgUnitListState, ["loading"], true);
    const error = get(orgUnitListState, ["error"], false);
    if (!loading && !error) {
      const orgRecords = get(orgUnitListState, ["data", "records"], {});
      const workspaceToorg = orgRecords?.reduce((acc: any, org: any) => {
        const workspaceId = `w_${org.workspace_id}`;
        if (acc.hasOwnProperty(workspaceId)) {
          acc[workspaceId] = [...acc[workspaceId], org.id];
        } else {
          acc[workspaceId] = [org.id];
        }
        return acc;
      }, {});
      handleSettingsChange("", workspaceToorg, "workspace_to_org");
    }
  }, [orgUnitListState]);

  useEffect(() => {
    if (profileId !== "new" && !trellisProfileDetailsState.isLoading && !trellisProfileDetailsState.error) {
      const data: any = get(trellisProfileDetailsState, ["data"], {});
      if (data.associated_ou_ref_ids?.length > 0) {
        dispatch(
          OrganizationUnitList(
            {
              filter: {
                ref_id: data.associated_ou_ref_ids
              }
            },
            ORG_UNIT_LIST_ID
          )
        );
      }
      setTrellisProfile(new RestTrellisScoreProfile(data));
      setSelectedSection(new RestTrellisProfileSections({ name: "basicInfo" }));
      setLoading(false);
    }
  }, [trellisProfileDetailsState]);

  useEffect(() => {
    if (updating) {
      const { saveClicked, isSaving, error } = profileSavingState;
      if (saveClicked && !isSaving) {
        if (!error) {
          notification.success({
            message: `${trellisProfile.name} ${trellisProfile.id ? "Updated" : "created"} Successfully`
          });
          history.push(`${getBaseUrl()}${TRELLIS_SCORE_PROFILE_ROUTES._ROOT}`);
        } else {
          notification.error({ message: error });
        }
        setUpdating(false);
      }
    }
  }, [profileSavingState]);

  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case CANCEL_ACTION_KEY:
          history.goBack();
          return {
            hasClicked: false
          };
        case SAVE_ACTION_KEY:
          if (trellisProfile.validate) {
            if (trellisProfile.id) {
              if (!hasSaveAccess) {
                dispatch(trellisProfilesPartialUpdateAction(trellisProfile.id!, trellisProfile.json));
              } else dispatch(trellisProfilesUpdateAction(trellisProfile.id!, trellisProfile.json));
            } else {
              dispatch(createTrellisProfileAction(trellisProfile.json));
            }
            setUpdating(true);
            return {
              hasClicked: false,
              disabled: true,
              showProgress: true
            };
          }
          notification.error({ message: "Please select effort investment categories for features in Impact Tab" });
          return {
            hasClicked: false
          };
        default:
          return null;
      }
    });
  }, [onActionButtonClick]);

  const menuChangeHandler = useCallback(
    (value: any) => {
      setSelectedMenu(value.key);
      const section = trellisProfile?.sections?.find((item: any) => item.name === value.key);
      if (section) {
        setSelectedSection(section as RestTrellisProfileSections);
      }
    },
    [trellisProfile]
  );

  const handleFeatureValueChange = (value: any, name: string, type: string) => {
    let feature = selectedSection?.features?.find(feature => feature.name === name);
    if (feature) {
      switch (type) {
        case "rating":
          feature.lower_limit_percentage = value[0];
          feature.upper_limit_percentage = value[1];
          break;
        case "max_value":
          feature.max_value = value;
          break;
        case "enabled":
          feature.enabled = value;
          break;
        case "category":
          if (Array.isArray(value)) {
            feature.ticket_categories = value;
          } else {
            feature.ticket_categories = [value];
          }
          break;
      }
      setTrellisProfile(
        new RestTrellisScoreProfile({
          ...trellisProfile.json,
          sections: (trellisProfile?.sections || []).map(section => {
            if (section.name === selectedSection.name) {
              return selectedSection.json;
            }
            return section.json;
          })
        } as RestTrellisScoreProfile)
      );
    }
  };

  const handleFeatureValueReset = (name: string, type: string) => {
    let feature = selectedSection?.features?.find(feature => feature?.name === name);
    const data: RestTrellisScoreProfile | undefined = get(trellisProfileDetailsState, ["data"], undefined);
    const initialSection = (data?.sections || []).find(
      (section: RestTrellisProfileSections) => section?.name === selectedSection?.name
    );
    const initialFeature = initialSection?.features?.find(
      (feature: RestTrellisProfileFeatures) => feature.name === name
    );
    if (feature && initialFeature) {
      if (type == "max_value" && feature!.max_value !== initialFeature.max_value) {
        feature!.max_value = initialFeature.max_value;
        setTrellisProfile(
          new RestTrellisScoreProfile({
            ...trellisProfile.json,
            sections: (trellisProfile?.sections || []).map(section => {
              if (section?.name === selectedSection?.name) {
                return selectedSection.json;
              }
              return section.json;
            })
          } as RestTrellisScoreProfile)
        );
      }
    }
  };

  const handleBasicInfoChange = useCallback(
    (newValue: any) => {
      setTrellisProfile(
        new RestTrellisScoreProfile({
          ...trellisProfile.json,
          ...newValue
        })
      );
    },
    [trellisProfile]
  );

  const handleSettingsChange = useCallback(
    (section_name: string, value: any, type: string) => {
      if (section_name) {
        const sectionToEdit = (trellisProfile?.sections || []).find(
          section => section_name === get(TRELLIS_SECTION_MAPPING, [section?.name || ""], section?.name)
        );
        if (sectionToEdit) {
          if (type === "weight") {
            sectionToEdit.weight = value;
          }
          if (type === "enable") {
            sectionToEdit.enabled = value;
            (sectionToEdit.features || []).forEach(feature => {
              feature.enabled = value;
            });
          }
          setTrellisProfile(
            new RestTrellisScoreProfile({
              ...trellisProfile.json,
              sections: (trellisProfile?.sections || []).map(section => {
                if (section_name === get(TRELLIS_SECTION_MAPPING, [section?.name || ""], section?.name)) {
                  return sectionToEdit.json;
                }
                return section.json;
              })
            } as RestTrellisScoreProfile)
          );
        } else {
          const newSection = new RestTrellisProfileSections({
            name: section_name,
            [type]: value
          });
          setTrellisProfile(
            new RestTrellisScoreProfile({
              ...trellisProfile.json,
              sections: [...trellisProfile?.sections, newSection]
            } as RestTrellisScoreProfile)
          );
        }
      } else if (type === "effort_investment_profile") {
        setTrellisProfile(
          new RestTrellisScoreProfile({
            ...trellisProfile.json,
            effort_investment_profile_id: value,
            sections: (trellisProfile?.sections || []).map(section => {
              return {
                ...section.json,
                features: (section?.features || []).map(
                  feature =>
                    new RestTrellisProfileFeatures({
                      ...feature.json,
                      ticket_categories: []
                    })
                )
              };
            })
          } as RestTrellisScoreProfile)
        );
      } else {
        setTrellisProfile(
          new RestTrellisScoreProfile({
            ...trellisProfile.json,
            [type]: value
          } as RestTrellisScoreProfile)
        );
      }
    },
    [trellisProfile]
  );

  if (loading || apiLoading) {
    return <Loader />;
  }

  const content = () => {
    switch (selectedMenu) {
      case TRELLIS_PROFILE_MENU.BASIC_INFO:
        return (
          <BasicInfo
            profile={trellisProfile}
            handleChanges={handleBasicInfoChange}
            profilesList={profilesList}
            disabled={trellisProfile.predefined_profile}
          />
        );
      case TRELLIS_PROFILE_MENU.ASSOCIATIONS:
        return (
          <Associations
            profile={trellisProfile}
            handleChanges={handleSettingsChange}
            profilesList={profilesList}
            ticketCategorizationData={ticketCategorizationData}
          />
        );
      case TRELLIS_PROFILE_MENU.FACTORS_WEIGHTS:
        return <Factors profile={trellisProfile} handleChanges={handleSettingsChange} />;
      default:
        return (
          <TrellisProfileSection
            section={selectedSection}
            handleFeatureValueReset={handleFeatureValueReset}
            handleFeatureValueChange={handleFeatureValueChange}
            ticketCategorizationData={ticketCategorizationData}
            ticketCategoryId={trellisProfile.effort_investment_profile_id}
          />
        );
    }
  };

  return (
    <div className="dev-score-profile-container">
      <TrellisProfileMenu onChange={menuChangeHandler} updatedAt={trellisProfile.updated_at} />
      {content()}
    </div>
  );
};

export default TrellisProfile;
