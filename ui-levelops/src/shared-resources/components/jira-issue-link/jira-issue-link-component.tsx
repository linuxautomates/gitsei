import { joinUrl } from "../../../utils/stringUtils";
import { SvgIcon } from "../index";
import React, { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import "./jira-issue-link.style.scss";
import { ProjectPathProps } from "classes/routeInterface";
import { getDashboardsPage } from "constants/routePaths";

interface JiraIssueLinkProps {
  link: string; // levelops summary link
  ticketKey: string; // issue ticket key
  integrationUrl?: string; // integration url used for direct link
}

export const JiraIssueLink: React.FC<JiraIssueLinkProps> = ({ link, ticketKey, integrationUrl }) => {
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
    <div className="jira-issue-row">
      <Link to={`${getDashboardsPage(projectParams)}/${link}`} target={"_blank"} rel="noopener noreferrer">
        {ticketKey}
      </Link>
      <div className="jira-external-link">
        {integrationUrl && (
          <a href={joinUrl(integrationUrl, `/browse/${ticketKey}`)} target={"_blank"} rel="noopener noreferrer">
            <SvgIcon icon="externalLink" style={externalLinkStyle} />
          </a>
        )}
      </div>
    </div>
  );
};
