import React, { useCallback } from "react";
import { useDispatch } from "react-redux";
import { cloneDeep, forEach } from "lodash";
import GridLayout, { Layout, WidthProvider } from "react-grid-layout";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import {
  NEW_SCHEME_ID,
  TICKET_CATEGORIZATION_SCHEME,
  UNCATEGORIZED_ID_SUFFIX
} from "../../constants/ticket-categorization.constants";
import { getCategoriesLayout } from "./helper";
import CategoryCard from "../scheme-drilldown/category-card/CategoryCard";
import "./CategoryRank.style.scss";

const ResponsiveReactGridLayout = WidthProvider(GridLayout);
interface CategoryRankComponentProps {
  profile: RestTicketCategorizationScheme;
}

const CategoryRankComponent: React.FC<CategoryRankComponentProps> = ({ profile }) => {
  const dispatch = useDispatch();

  const categories = (profile?.categories ?? []).filter(category => category?.id !== UNCATEGORIZED_ID_SUFFIX);

  const handleCategoriesUpdate = useCallback(
    (updatedCategories: any[]) => {
      const uncategorizedCategory = (profile?.categories ?? []).find(
        category => category.id === UNCATEGORIZED_ID_SUFFIX
      );
      if (!!uncategorizedCategory) {
        updatedCategories.push(uncategorizedCategory?.json);
      }
      profile.categories = updatedCategories;
      if (!profile?.id)
        dispatch(genericRestAPISet(profile?.json, TICKET_CATEGORIZATION_SCHEME, "create", NEW_SCHEME_ID));
      else dispatch(genericRestAPISet(profile?.json, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id));
    },
    [profile]
  );

  const handleLayoutChange = (layout: Layout[]) => {
    layout.sort((a, b) => {
      if (a.y === b.y) return a.x - b.x;
      return a.y - b.y;
    });
    let newCategories: any[] = [];
    forEach(layout, category => {
      const ccategory = (categories || []).find(existingCategory => existingCategory?.id === category.i);
      if (!!ccategory) {
        newCategories.push(cloneDeep(ccategory.json));
      }
    });
    handleCategoriesUpdate(newCategories);
  };

  const layout = getCategoriesLayout(categories || []);

  const getCategory = (id: string) => {
    return (categories || []).find(category => category.id === id);
  };

  return (
    <div className="rank-container">
      <ResponsiveReactGridLayout
        autoSize
        verticalCompact
        layout={layout}
        cols={3}
        isDraggable={true}
        onDragStop={handleLayoutChange}
        rowHeight={40}
        isDroppable={true}
        className="layout">
        {layout.map((layout: any, index: number) => (
          <div key={layout?.i} id={layout?.i}>
            <CategoryCard category={getCategory(layout?.i)} profile={profile} rank={index + 1} />
          </div>
        ))}
      </ResponsiveReactGridLayout>
    </div>
  );
};

export default CategoryRankComponent;
