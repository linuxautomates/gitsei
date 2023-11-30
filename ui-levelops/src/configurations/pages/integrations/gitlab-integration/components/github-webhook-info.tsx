import { Alert } from "antd";
import { USER_ORG } from "constants/localStorageKeys";
import React, { useMemo } from "react";
import { AntCard } from "shared-resources/components";

interface GithubWebhookInfoProps {
  integration_id: string;
}

const GithubWebhookInfo: React.FC<GithubWebhookInfoProps> = ({ integration_id }) => {
  const company = localStorage.getItem(USER_ORG);
  const description = (
    <div>
      Use this information to manually add Webhook -
      <ol className="mt-5">
        <li>
          <b>URL</b>
          {` : https://dev.webhooks.propelo.ai/v1/webhooks/github/notifications/${company}/${integration_id}`}
        </li>
        <li>
          <b>Event types to register on Github</b> : Issues, Project columns, Project cards, Projects, Pull requests,
          Pull request reviews, Pushes
        </li>
      </ol>
    </div>
  );

  const style: any = useMemo(
    () => ({
      textAlign: "start",
      width: "70vw",
      margin: "0 auto 10px auto"
    }),
    []
  );

  return (
    <div style={style}>
      <Alert message="Informational Notes" description={description} type="info" showIcon />
    </div>
  );
};

export default GithubWebhookInfo;
