import React, { useEffect } from "react";
import { notification } from "antd";
import { useHistory, useParams } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";
import CreateCategoriesContainer from "../../components/create-edit-categories/CreateCategoriesContainer";
import EditCategoriesContainer from "../../components/create-edit-categories/EditCategoriesContainer";
import { NEW_SCHEME_ID } from "../../constants/ticket-categorization.constants";

const CategoriesContainer: React.FC = () => {
  const history = useHistory();
  const params = useParams();
  const schemeId = (params as any).id;
  const categoryId = (params as any).categoryId;
  const isNew = schemeId === NEW_SCHEME_ID;

  useEffect(() => {
    if (schemeId === undefined || schemeId === "undefined" || categoryId === undefined || categoryId === "undefined") {
      notification.error({ message: "Category not found!" });
      history.push(WebRoutes.ticket_categorization.list());
    }
  }, []);

  return isNew ? (
    <CreateCategoriesContainer categoryId={categoryId as any} />
  ) : (
    <EditCategoriesContainer schemeId={schemeId} categoryId={categoryId as any} />
  );
};

export default CategoriesContainer;
