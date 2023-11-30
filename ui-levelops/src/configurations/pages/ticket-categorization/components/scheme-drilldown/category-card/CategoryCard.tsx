import { Dropdown, Icon, Menu, Popconfirm } from "antd";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import {
  CATEGORY_DELETE_MESSAGE,
  NEW_SCHEME_ID,
  TICKET_CATEGORIZATION_SCHEME
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { ProfileUpdateContext } from "dashboard/pages/context";
import { get } from "lodash";
import React, { useCallback, useContext, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { WebRoutes } from "routes/WebRoutes";
import { AntCard, AntText } from "shared-resources/components";
import TooltipWithTruncatedTextComponent from "shared-resources/components/tooltip-with-truncated-text/TooltipWithTruncatedTextComponent";
import "./category-card.style.scss";

interface CategoryCardProps {
  category: any;
  rank: number;
  profile: RestTicketCategorizationScheme;
}
const DEFAULT_RANGE = ["0", "0"];
const CategoryCard: React.FC<CategoryCardProps> = ({ category, rank, profile }) => {
  const { handleUpdate: handleSchemeUpdate } = useContext(ProfileUpdateContext);
  const history = useHistory();
  const dispatch = useDispatch();

  const handlePopoverConfirm = useCallback(
    categoryId => {
      profile.removeCategory(categoryId);
      profile.categories = profile?.categories?.map(category => ({ ...category.json })) as any[];
      onSchemeUpdate();
    },
    [profile]
  );

  const onSchemeUpdate = useCallback(() => {
    dispatch(genericRestAPISet(profile?.json, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id));
    handleSchemeUpdate(profile?.json);
  }, [profile]);

  const handleMenuClick = useCallback(
    (key: string, categoryId: string) => {
      switch (key) {
        case "edit":
          history.push(
            WebRoutes.ticket_categorization.scheme.category.details(profile?.id || NEW_SCHEME_ID, categoryId)
          );
          break;
        case "delete":
          break;
      }
    },
    [profile]
  );
  const renderMenu = useMemo(() => {
    return (
      <>
        <div className="category-card-edit mr-10" onClick={(e: any) => handleMenuClick("edit", category?.id)}>
          <Icon type={"edit"} />
        </div>
        <div className="category-card-edit" onClick={(e: any) => handleMenuClick("delete", category?.id)}>
          <Popconfirm
            title={CATEGORY_DELETE_MESSAGE}
            placement="left"
            onConfirm={event => handlePopoverConfirm(category?.id)}>
            <Icon type={"delete"} />
          </Popconfirm>
        </div>
      </>
    );
  }, [category]);

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

  const idealRange = getIdealRange(category);

  return (
    <div className="category-card">
      <p className="category-card-rank">{`Rank ${rank}`}</p>
      <AntCard
        style={category?.background_color ? { borderLeft: `4px solid ${category?.background_color}` } : {}}
        title={
          <AntText>
            {category?.name}{" "}
            <div className="category-content-container-description">
              <TooltipWithTruncatedTextComponent title={category?.description} allowedTextLength={54} />
            </div>
          </AntText>
        }
        extra={<div className="flex align-center ml-10">{renderMenu}</div>}>
        <div className="category-content-container">
          <div className="category-card-rank-container">
            <p className="category-card-rank">{`Ideal range ${idealRange[0]}% -${idealRange[1]}%`}</p>
            <Icon type="drag" className="category-card-rank-container-icon" />
          </div>
        </div>
      </AntCard>
    </div>
  );
};

export default CategoryCard;
