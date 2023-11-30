import React from "react";
import { RouteComponentProps, withRouter } from "react-router-dom";
import { AntButton, AntText } from "shared-resources/components";
import { Avatar, Icon } from "antd";
import "./product-stage.style.scss";
import { getSettingsPage } from "constants/routePaths";

interface ProductStageComponentProps extends RouteComponentProps {
  onMapIntegration: (stage: any) => void;
  stage: any;
  className?: string;
  mappings: any;
  collapsed: boolean;
  expandedComponent: JSX.Element;
  integrations: any;
  onExpand: () => void;
  status?: string;
}
export const ProductStageComponent: React.FC<ProductStageComponentProps> = (props: ProductStageComponentProps) => {
  const handleIntegrationFlow = (e: any) => {
    e.preventDefault();
    props.onMapIntegration(props.stage);
  };

  const handleAddNewIntegration = (e: any) => {
    e.preventDefault();
    props.history.push(
      `${getSettingsPage()}/integrations/add-integration-page?return=/admin/products/edit-product-page?step=1&product=${
        props.stage.product_id
      }`
    );
  };

  const { className, stage, collapsed, expandedComponent, integrations } = props;
  let integrationsAvailable = integrations.filter((integ: any) => stage.integration_types.includes(integ.application));
  return (
    <div className={`${className} ${className}__container flex direction-column justify-start`}>
      <div className={`flex justify-space-between align-center`}>
        <div className={`flex align-center`}>
          <Icon className="mr-5" type={collapsed ? "plus" : "minus"} onClick={props.onExpand} />
          <AntText className={`${className}__name`}>{stage.name.replace("_", " ").toUpperCase()}</AntText>
        </div>
        <div className={`flex align-center`}>
          <AntButton
            type="link"
            onClick={handleAddNewIntegration}
            className={`${className}__action mr-5 text-uppercase`}>
            add integrations
          </AntButton>
          {integrationsAvailable.length > 0 && (
            <AntButton
              onClick={handleIntegrationFlow}
              type="link"
              className={`${className}__action mr-5 text-uppercase`}>
              map integrations
            </AntButton>
          )}
          <Avatar style={{ backgroundColor: "var(--harness-blue)" }} size={"small"}>
            {props.mappings.length}
          </Avatar>
        </div>
      </div>
      <div>{!collapsed && expandedComponent}</div>
    </div>
  );
};

ProductStageComponent.defaultProps = {
  className: "product-stage",
  collapsed: true,
  mappings: [],
  expandedComponent: <div>Expanded</div>
};

export default React.memo(withRouter(ProductStageComponent));
