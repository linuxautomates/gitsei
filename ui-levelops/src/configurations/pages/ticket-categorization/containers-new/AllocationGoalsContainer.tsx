import React, { useCallback, useState } from "react";
import { Collapse, Tag, Switch } from "antd";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import {
  ALLOCATION_GOALS_DESCRIPTION,
  NEW_SCHEME_ID,
  TICKET_CATEGORIZATION_SCHEME,
  UNCATEGORIZED_ID_SUFFIX
} from "../constants/ticket-categorization.constants";
import { AntButton } from "shared-resources/components";
import IdealRangeSlider from "./IdealRangeSlider";
import { cloneDeep, forEach, get } from "lodash";
import { useDispatch } from "react-redux";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import "./allocationGoals.styles.scss";
interface AllocationGoalsContainerProps {
  profile: RestTicketCategorizationScheme;
  hasCustomHeader?: boolean;
}

const DEFAULT_RANGE = ["0", "0"];
const AllocationGoalsContainer: React.FC<AllocationGoalsContainerProps> = ({ profile, hasCustomHeader }) => {
  const categories = profile?.categories || [];
  const defaultOpenPanels = categories.map((c, index) => index + "");
  const [activePanel, setActivePanel] = useState<string | string[]>(defaultOpenPanels);
  const dispatch = useDispatch();

  const getTagColor = (category: RestTicketCategorizationCategory) => {
    if (category?.id === UNCATEGORIZED_ID_SUFFIX) {
      return profile?.uncategorized_color;
    }
    return category?.background_color;
  };

  const handleSetActivePanel = (value: string | string[]) => {
    setActivePanel(value);
  };

  const handleUpdate = useCallback(
    (updatedCategory: any) => {
      profile?.addCategory(updatedCategory);
      if (!profile?.id)
        dispatch(
          genericRestAPISet({ ...profile?.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID)
        );
      else
        dispatch(
          genericRestAPISet({ ...profile?.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id)
        );
    },
    [profile]
  );

  const handleChangeToCategory = (categoryId: string, key: string, value: any) => {
    const category = (categories || []).find(category => category?.id === categoryId);
    if (!!category) {
      let newCategory = cloneDeep(category);
      (newCategory as any)[key] = value;
      handleUpdate(newCategory.json);
    }
  };

  const handleResetIdealRange = () => {
    const clonnedCategories = cloneDeep(categories);
    const distributedLimit = Math.floor(100 / (categories || []).length).toString();
    forEach(clonnedCategories, category => {
      category.ideal_range = ["0", distributedLimit];
    });

    profile.categories = clonnedCategories.map(category => category.json) as any;

    if (!profile?.id)
      dispatch(
        genericRestAPISet({ ...profile?.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID)
      );
    else
      dispatch(genericRestAPISet({ ...profile?.json, draft: true }, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id));
  };

  const getIdealRange = useCallback(
    (category: RestTicketCategorizationCategory) => {
      let idealRange = category?.ideal_range;
      if (!idealRange) return DEFAULT_RANGE;
      if (idealRange.hasOwnProperty("min")) {
        return [get(idealRange || {}, ["min"], 0), get(idealRange || {}, ["max"], 0)];
      }
      if (idealRange[0] === undefined) return DEFAULT_RANGE;
      return idealRange;
    },
    [profile]
  );

  return (
    <div className="allocation-goals-container">
      {!hasCustomHeader && (
        <div className="header">
          <h2 className="allocation-title">Allocation Goals</h2>
          <p className="allocation-subtitle">{ALLOCATION_GOALS_DESCRIPTION}</p>
        </div>
      )}
      <div className="panel-container">
        <Collapse activeKey={activePanel} onChange={handleSetActivePanel}>
          {categories.map((category, index) => {
            const idealRange = getIdealRange(category);
            return (
              <Collapse.Panel
                key={index}
                header={
                  <Tag color={getTagColor(category)} className="panel-tag-text">
                    {category?.name}
                  </Tag>
                }
                extra={
                  <div className="flex">
                    <span
                      className="mr-10"
                      style={{ color: "#595959" }}>{`Ideal range ${idealRange[0]}% - ${idealRange[1]}%`}</span>
                    <Switch
                      checked={category?.enabled}
                      onChange={(value: boolean) => handleChangeToCategory(category?.id || "", "enabled", value)}
                    />
                  </div>
                }>
                <IdealRangeSlider
                  index={index}
                  categoryId={category?.id || index + ""}
                  idealRange={idealRange}
                  handleChangeToCategory={(value: string[]) =>
                    handleChangeToCategory(category?.id || "", "ideal_range", value)
                  }
                />
              </Collapse.Panel>
            );
          })}
        </Collapse>
        <div className="footer">
          <AntButton onClick={handleResetIdealRange}>Reset Goals</AntButton>
          <p>Distributes efforts equally between all categories. </p>
        </div>
      </div>
    </div>
  );
};

export default AllocationGoalsContainer;
