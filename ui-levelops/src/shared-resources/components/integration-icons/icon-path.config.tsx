import React from "react";
import { IconName } from "@harness/uicore";
import TestRailPath from "./icon-path/testrail.path.svg";
import PerforceHelixCorePath from "./icon-path/perforce-helix-core.path.svg";
import PerforceHelixSwarmPath from "./icon-path/perforce-helix-swarm.path.svg";
import PerofrceHelixServer from "./icon-path/perforce_helix_server.path.svg";
import Gerrit from "./icon-path/gerrit.path.svg";
import Salesforce from "./icon-path/salesforce.path.svg";
import Zendesk from "./icon-path/zendesk.path.svg";
import Praetorian from "./icon-path/praetorian.path.svg";
import Postgres from "./icon-path/postgres.path.svg";
import NCC from "./icon-path/nccgroup.path.svg";
import Microsoft from "./icon-path/microsoft.path.svg";
import GitLab from "./icon-path/gitlab.path.svg";
import BitBucket from "./icon-path/bitbucket.path.svg";

type IconPathType = {
  temp_props: {
    type: string;
    viewBox: string;
  }
  icon_name?: IconName,
  icon_path?: JSX.Element;
}

export const ICON_PATH: IconPathType[] = [
  { temp_props: { type: "tenable", viewBox: "0 0 75 75" }, icon_name: 'Tenable' },
  { temp_props: { type: "github", viewBox: "0 0 438.549 438.549" }, icon_name: 'github-action-plugin' },  
  { temp_props: { type: "slack", viewBox: "0 0 24 24" }, icon_name: 'service-slack' },
  { temp_props: { type: "salesforce", viewBox: "0 0 90 50" }, icon_path: <Salesforce /> },  
  { temp_props: { type: "snyk", viewBox: "0, 0, 400,405.7142857142857" }, icon_name: 'Snyk' },
  { temp_props: { type: "jira", viewBox: "2.59 0 214.09101008 224" }, icon_name: 'service-jira' },
  { temp_props: { type: "pagerduty", viewBox: "0, 0, 400,400" }, icon_name: 'service-pagerduty' },
  { temp_props: { type: "splunk", viewBox: "0 0 55 65" }, icon_name: 'service-splunk'},
  { temp_props: { type: "postgres", viewBox: "0 0 48 48" }, icon_path: <Postgres /> },
  { temp_props: { type: "circleci", viewBox: "0 0 64 64" }, icon_name: 'service-circleci' },
  { temp_props: { type: "datadog", viewBox: "0 0 64 64" }, icon_name: 'service-datadog' },
  { temp_props: { type: "jenkins", viewBox: "0, 0, 400,553.5135135135135" }, icon_name: 'service-jenkins' },
  { temp_props: { type: "zendesk", viewBox: "0 0 64 64" }, icon_path: <Zendesk /> },
  { temp_props: { type: "microsoft", viewBox: "0, 0, 400,398.2222222222222" }, icon_path: <Microsoft /> },
  { temp_props: { type: "praetorian", viewBox: "0, 0, 400,178.343949044586" }, icon_path: <Praetorian /> },
  { temp_props: { type: "brakeman", viewBox: "0, 0, 400,400" }, icon_name: 'brakeman' },
  { temp_props: { type: "ncc", viewBox: "0 0 400.000000 400.000000" }, icon_path: <NCC /> },
  { temp_props: { type: "gerrit", viewBox: "0, 0, 52, 52" }, icon_path: <Gerrit /> },
  { temp_props: { type: "bitbucket", viewBox: "90, 115, 335, 305" }, icon_path: <BitBucket /> },
  { temp_props: { type: "sonarqube", viewBox: "90, 115, 335, 305" }, icon_name: 'SonarQube' },
  { temp_props: { type: "perforce_helix_server", viewBox: "0 0 32 32" }, icon_path: <PerofrceHelixServer /> },
  { temp_props: { type: "perforce_helix_core", viewBox: "0, 0, 50, 50" }, icon_path: <PerforceHelixCorePath /> },
  { temp_props: { type: "perforce_helix_swarm", viewBox: "0 1 42 42" }, icon_path: <PerforceHelixSwarmPath /> },
  { temp_props: { type: "testrail", viewBox: "0, 0, 300, 150" }, icon_path: <TestRailPath /> },
  { temp_props: { type: "azure_devops", viewBox: "0 0 630 512" }, icon_name: 'service-azdevops' },
  { temp_props: { type: "checkmarx", viewBox: "0 0 24 24" }, icon_name: 'Checkmarx' },
  { temp_props: { type: "cxsast", viewBox: "0 0 24 24" }, icon_name: 'Checkmarx' },
  { temp_props: { type: "gitlab", viewBox: "0, 0, 64, 64" }, icon_path: <GitLab /> },
  { temp_props: { type: "coverity", viewBox: "0 0 256 257" }, icon_name: 'coverity'},
  { temp_props: { type: "droneci", viewBox: "0 0 256 257" }, icon_name: 'ci-solid' },
  { temp_props: { type: "ms_teams", viewBox: "0 0 256 257" }, icon_name: 'service-microsoft-teams' },
  { temp_props: { type: "harness", viewBox: "0 0 256 257" }, icon_name: 'harness' },
  { temp_props: { type: "github_actions", viewBox: "0 0 256 257" }, icon_name: 'github-actions' },
  { temp_props: { type: "levelops", viewBox: "0 0 256 257" }, icon_name: 'harness' }
];
