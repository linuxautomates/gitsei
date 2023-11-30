import RBACNoAccessScreen from "components/RBACNoAccessScreen/RBACNoAccessScreen";
import { useHasViewConfigPermission } from "custom-hooks/HarnessPermissions/useHasViewConfigPermission";
import { getIsStandaloneApp } from "helper/helper";
import React from "react";

const withHarnessPermission = (WrappedComponent: React.ComponentType<any>) => {
  const WithHarnessPermission = (props: any) => {
    if (getIsStandaloneApp()) return <WrappedComponent {...props} />;
    return <RenderWithHarnessPermission Component={WrappedComponent} {...props} />;
  };
  return WithHarnessPermission;
};

export default withHarnessPermission;

const RenderWithHarnessPermission = ({ Component, ...props }: any) => {
  const hasViewAccess = useHasViewConfigPermission();
  if (hasViewAccess) return <Component {...props} />;
  return <RBACNoAccessScreen />;
};
