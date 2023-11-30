import React, { useCallback, useMemo, useRef, useState } from "react";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import BreadcrumbComponent from "core/containers/header/BreadcrumbComponent";
import { cloneDeep } from "lodash";
import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { WebRoutes } from "routes/WebRoutes";
import { AntButton, AntModal } from "shared-resources/components";
import {
  ADD_CATEGORY,
  CATEGORY_EDIT_CREATE_MODEL_DESC,
  EDIT_CATEGORY,
  NEW_SCHEME_ID,
  TICKET_CATEGORIZATION_SCHEME
} from "../../constants/ticket-categorization.constants";
import { getBreadcumsForCategoryPage } from "../../helper/getBreadcumsForCategoryPage";
import { categoryBasicInfo, CategoryBasicInfoTypes, EIConfigurationTabs } from "../../types/ticketCategorization.types";
import "./CategoryCreateEditModal.scss";
import CategoryStepsComponent from "./CategoryStepsComponent";
interface CategoryCreateEditModalProps {
  profile: RestTicketCategorizationScheme;
  category: RestTicketCategorizationCategory;
  handleUpdate: (updatedScheme: any) => void;
  handleSave: () => void;
}

const CategoryCreateEditModal: React.FC<CategoryCreateEditModalProps> = ({
  profile,
  category,
  handleUpdate,
  handleSave
}) => {
  const history = useHistory();
  const dispatch = useDispatch();
  const [currentStep, setCurrentStep] = useState<number>(0);
  const next = () => setCurrentStep(prev => prev + 1);
  const prev = () => setCurrentStep(prev => prev - 1);
  const initialCategorystate = useRef<any>();
  const initialProfileState = useRef<any>();
  const saveClicked = useRef<boolean>(false);
  const cancelClicked = useRef<boolean>(false);

  const updateState = () => {
    if (!profile?.id)
      dispatch(
        genericRestAPISet(
          { ...(profile?.json || {}), draft: true },
          TICKET_CATEGORIZATION_SCHEME,
          "create",
          NEW_SCHEME_ID
        )
      );
    else
      dispatch(
        genericRestAPISet({ ...(profile?.json || {}), draft: true }, TICKET_CATEGORIZATION_SCHEME, "get", profile?.id)
      );
  };

  const handleClear = () => {
    if (!initialCategorystate.current?.name) {
      profile?.removeCategory(category?.id);
      profile.categories = profile?.categories?.map(category => ({ ...category.json })) as any[];
      updateState();
    } else {
      restoreInitialState();
    }
  };

  const restoreInitialState = () => {
    let categories = (profile.categories = cloneDeep(
      profile?.categories?.map(category => ({ ...category.json })) as any[]
    ));
    let categoryIndex = categories?.findIndex(rcategory => rcategory.id === category?.id);
    if (categoryIndex !== undefined) {
      categories[categoryIndex] = initialCategorystate.current;
    }
    profile.categories = categories;
    updateState();
  };

  useEffect(() => {
    if (category) {
      initialCategorystate.current = cloneDeep(category?.json);
    }
    if (profile) {
      initialProfileState.current = cloneDeep(profile);
    }
    return () => {
      if (!initialCategorystate.current?.name && !saveClicked.current) {
        handleClear();
      } else if (!cancelClicked.current && !saveClicked.current) {
        if (!profile?.id)
          dispatch(
            genericRestAPISet(
              { ...(initialProfileState?.current?.json || {}), draft: true },
              TICKET_CATEGORIZATION_SCHEME,
              "create",
              NEW_SCHEME_ID
            )
          );
        else
          dispatch(
            genericRestAPISet(
              { ...(initialProfileState?.current?.json || {}), draft: true },
              TICKET_CATEGORIZATION_SCHEME,
              "get",
              profile?.id
            )
          );
      }
    };
  }, []);

  const handleCancel = useCallback(
    (e?: any) => {
      e?.preventDefault?.();
      cancelClicked.current = true;
      handleClear();
      history.push(
        WebRoutes.ticket_categorization.scheme.edit(profile?.id || NEW_SCHEME_ID, EIConfigurationTabs.CATEGORIES)
      );
    },
    [profile]
  );

  const handleChanges = useCallback(
    (key: categoryBasicInfo, value: string | boolean) => {
      if (key === CategoryBasicInfoTypes.BACKGROUND_COLOR) {
        profile?.updateCategoryColorMapping(category?.background_color, value as string);
      }
      (category as any)[key] = value;
      handleUpdate(category?.json || {});
    },
    [category, handleUpdate]
  );

  const handleSaveClicked = () => {
    saveClicked.current = true;
    handleSave();
  };

  const renderFooter = useMemo(() => {
    return (
      <>
        <AntButton onClick={handleCancel}>Cancel</AntButton>
        {currentStep === 1 && (
          <AntButton type="default" onClick={prev}>
            Back
          </AntButton>
        )}
        <AntButton onClick={currentStep === 1 ? handleSaveClicked : next} type="primary" disabled={!category?.name}>
          {currentStep === 1 ? "Save" : "Next"}
        </AntButton>
      </>
    );
  }, [currentStep, category]);

  return (
    <AntModal
      wrapClassName={"scheme-modal-container"}
      visible={true}
      onCancel={handleCancel}
      maskClosable={false}
      title={
        <div className="flex direction-column">
          <BreadcrumbComponent
            breadcrumbs={getBreadcumsForCategoryPage(
              (profile.id as any) || NEW_SCHEME_ID,
              profile.name || "Profile",
              category.id as any,
              category?.name || "Category"
            )}
          />
          {!(initialCategorystate.current?.name || "") ? ADD_CATEGORY : EDIT_CATEGORY}
        </div>
      }
      footer={renderFooter}
      closable={false}>
      <p className="ml-20">{CATEGORY_EDIT_CREATE_MODEL_DESC}</p>
      <CategoryStepsComponent
        category={category}
        profile={profile}
        handleChanges={handleChanges}
        handleUpdate={handleUpdate}
        currentStep={currentStep}
      />
    </AntModal>
  );
};

export default CategoryCreateEditModal;
