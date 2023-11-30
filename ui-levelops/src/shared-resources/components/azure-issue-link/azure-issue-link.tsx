import { joinUrl } from "../../../utils/stringUtils";
import { SvgIcon } from "../index";
import React, { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import "./azure-issue-link.scss";
import { ProjectPathProps } from "classes/routeInterface";
import { getDashboardsPage } from "constants/routePaths";

interface AzureIssueLinkProps {
  link: string; // levelops summary link
  workItemId: string;
  integrationUrl?: string; // integration url used for direct link
  organization: string;
  project: string;
}

export const AzureIssueLinkProps: React.FC<AzureIssueLinkProps> = ({
  link,
  workItemId,
  integrationUrl,
  organization,
  project
}) => {
  const projectParams = useParams<ProjectPathProps>();
  // have to use inline style to override SvgIcon styling
  const externalLinkStyle = useMemo(
    () => ({
      width: 16,
      height: 16,
      align: "center"
    }),
    []
  );

  return (
    <div className="azure-issue-row">
      <Link to={`${getDashboardsPage(projectParams)}/${link}`} target={"_blank"} rel="noopener noreferrer">
        {workItemId}
      </Link>
      <div className="azure-external-link">
        {integrationUrl && (
          <Link
            to={{ pathname: joinUrl(integrationUrl, `/${project}/_workitems/edit/${workItemId}`) }}
            target={"_blank"}
            rel="noopener noreferrer">
            <SvgIcon icon="externalLink" style={externalLinkStyle} />
          </Link>
        )}
      </div>
    </div>
  );
};
