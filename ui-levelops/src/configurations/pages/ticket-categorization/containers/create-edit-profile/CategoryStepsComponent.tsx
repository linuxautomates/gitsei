import React from "react";
import { Steps } from "antd";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import { categoryBasicInfo, CategoryCreatEditStepsType } from "../../types/ticketCategorization.types";
import CategoryRuleContainer from "../../components/create-edit-categories/CategoryRuleContainer";
import CategoryBasicInfoComponent from "../../components/create-edit-categories/category-basic-info/CategoryBasicInfoComponent";
import "./CategoryStepsContainer.styles.scss";

interface CategoryStepsProps {
  category: RestTicketCategorizationCategory;
  currentStep: number;
  profile: RestTicketCategorizationScheme;
  handleChanges: (key: categoryBasicInfo, value: string | boolean) => void;
  handleUpdate: (updatedCategory: any) => void;
}

const CategoryStepsComponent: React.FC<CategoryStepsProps> = ({
  category,
  currentStep,
  profile,
  handleUpdate,
  handleChanges
}) => {
  const steps: CategoryCreatEditStepsType[] = [
    {
      title: "Basic Info",
      content: <CategoryBasicInfoComponent category={category} handleChanges={handleChanges} profile={profile} />
    },
    {
      title: "Filters",
      content: <CategoryRuleContainer category={category} handleUpdate={handleUpdate} />
    }
  ];

  return (
    <div className="category-steps-container">
      <div className="category-steps-container_steps">
        <Steps current={currentStep}>
          {steps.map(item => (
            <Steps.Step key={item.title} title={item.title} />
          ))}
        </Steps>
      </div>
      <div>{steps[currentStep].content}</div>
    </div>
  );
};

export default CategoryStepsComponent;
