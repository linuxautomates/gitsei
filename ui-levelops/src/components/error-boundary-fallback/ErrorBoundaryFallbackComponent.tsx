import React from "react";
import { Result, Typography } from "antd";
import { DEFAULT_ROUTES } from "constants/routePaths";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import Loader from "components/Loader/Loader";
import { useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";

const { Text } = Typography;

interface ErrorFallbackComponentProps {
  error: Error;
  info: React.ErrorInfo;
  clearError: () => void;
}

const ErrorFallbackComponent: React.FC<ErrorFallbackComponentProps> = (props: ErrorFallbackComponentProps) => {
  const projectParams = useParams<ProjectPathProps>();
  const onBackToHome = () => {
    const rbac = new LocalStoreService().getUserRbac()?.toLowerCase();
    props.clearError();
    switch (rbac) {
      case USERROLES.SUPER_ADMIN:
      case USERROLES.ADMIN:
        window.location.href = DEFAULT_ROUTES.ADMIN(projectParams);
        break;
      case USERROLES.ASSIGNED_ISSUES_USER:
        window.location.href = DEFAULT_ROUTES.ASSIGNED_ISSUES_USER();
        break;
      case USERROLES.AUDITOR:
        window.location.href = DEFAULT_ROUTES.AUDITOR();
        break;
      case USERROLES.LIMITED_USER:
        window.location.href = DEFAULT_ROUTES.LIMITED_USER();
        break;
      case USERROLES.RESTRICTED_USER:
        window.location.href = DEFAULT_ROUTES.RESTRICTED_USER();
        break;
      default:
        break;
    }
  };

  // @ts-ignore
  if (props.error.retryLoading) {
    return (
      <div className="centered" style={{ width: "100%", height: "250px" }}>
        <Loader />
      </div>
    );
  }

  return (
    <div className="centered" style={{ width: "100%", height: "100%" }}>
      <Result
        status="error"
        title={
          <Text style={{ fontSize: "1.2rem" }}>
            Oops, something bad happened, please <a onClick={props.clearError}>refresh</a>.
          </Text>
        }
        subTitle={
          <Text>
            <a onClick={onBackToHome}>Back to Home</a>
          </Text>
        }
      />
    </div>
  );
};

export default ErrorFallbackComponent;
