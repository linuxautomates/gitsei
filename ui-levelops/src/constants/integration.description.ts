export const TENABLE_DESC = `Managed in the cloud and powered by Nessus technology, Tenable.io provides the industry's most comprehensive vulnerability coverage with the ability to predict which security issues to remediate first. It’s your complete end-to-end vulnerability management solution.`;

export const GITHUB_REPO_DESC =
  'List of repositories to ingest. Use the format "owner/name". Leave blank to ingest all the repositories from collections accessible to the token user.';

export const BITBUCKET_REPO_DESC =
  'List of repositories to ingest. Use the format "name". Leave blank to ingest all the repositories from collections accessible to the token user.';

export const GITHUB_DESC = `GitHub is a source code management system using Git. SEI integration with GitHub can ingest when PRs are created or merged. Contributors can automated tasks such as sending assessments using Propels for GitHub`;

export const JIRA_JQL_DESC =
  "Only issues matched by that query will be ingested by SEI. Leave blank to ingest everything.";

export const JIRA_OPTIONS_DESC =
  "Exclude fields containing sensitive information from ingestion.\n Note that excluded fields cannot be evaluated for “hygiene” or adherence to best practices.";

export const JIRA_DESC = `Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking.

SEI can ingest Jira issues - Epics, Stories, and Bugs to create reports and automate tasks such as sending assessments using Propels. `;

export const TESTRAILS_DESC = `TestRail is a complete web-based test case management solution to efficiently manage, track,
     and organize your software testing efforts.`;

export const SONARQUBE_ORGANIZATION_DESC = "Collection name, required field when installing SonarCloud";

export const SONARQUBE_PROJECT_KEYS_DESC =
  "Enter one or more project keys to ingest. Leave empty to ingest all projects. Note that project keys are case sensitive";

export const PERFORCE_HELIX_SATELLITE_DESC =
  "Private on-premise integrations are configured on a SEI Satellite. Download the Satellite configuration file for this integration and follow the below link to update your SEI Satellite";

export const PREFORCE_HELIX_DESC = `Perforce Helix Server is used to ingest changelist activity from Helix Core and code review activity Helix Swarm`;

export const SLACK_DESC = `Slack is a proprietary business communication platform developed by Slack Technologies.

SEI integration with Slack can send notifications to users from the SEI platform about assessments, tasks, and reminders in order to automate some workflows `;

export const SALESFORCE_DESC = `Salesforce.com, inc. is an American cloud-based software company headquartered in San Francisco, California. It provides customer relationship management service and also sells a complementary suite of enterprise applications focused on customer service, marketing automation, analytics, and application development.`;

export const SNYK_DESC = `Snyk is a software development security platform for checking vulnerabilities and licenses in open source code. 

SEI integration ingests and centralizes vulnerability information reported by Snyk and automate workflows using Propels`;

export const PAGERDUTY_DESC = `PagerDuty is an incident response platform.

SEI integration with Pagerduty ingests the alerts and incidents information for providing insights and automation using Propels`;

export const SPLUNK_DESC = `Splunk is a software for searching, monitoring, and analyzing machine-generated big data.

SEI integration with Splunk enables fetching data from Splunk to provide insights and automate using Propels `;

export const POSTGRES_DESC = `Postgres is a relational database management system.

SEI integration with Postgres enables fetching data from Postgres to provide insights and automate using Propels `;

export const ZENDESK_DESC = `Zendesk is a service-first CRM company that builds software designed to improve customer relationships. As employees, we encourage each other to grow and innovate. As a company, we roll up our sleeves to plant roots in the communities we call home. 
    Our software is powerful and flexible, and scales to meet the needs of any business. Even yours.`;

export const GERRIT_DESC = `Gerrit is a free, web-based team code collaboration tool. Software developers in a team can review each other's modifications on their source code using a Web browser and approve or reject those changes. It integrates closely with Git, a distributed version control system.`;

export const BITBUCKET_DESC = `Bitbucket is a web-based version control repository hosting service owned by Atlassian, for source code and development projects that use either Mercurial or Git revision control systems. Bitbucket offers both commercial plans and free accounts.`;

export const CHECKMARX_DESC = `Checkmarx Static Analysis Solution`;

export const AZURE_DESC = `Azure DevOps Services - Boards, Git Repos, TFVC and Pipelines`;

export const GITLAB_DESC = `GitLab is a DevOps platform that provides git repos, CI/CD pipeline, issue management and more.`;

export const CIRCLECI_DESC = `CircleCI is a CI/CD platform`;

export const COVERITY_DESC = `Static analysis scanner from Synopsys`;

export const DRONECI_REPO_DESC =
  'List of repositories to ingest. Use the format "owner/name". Leave blank to ingest all the repositories from collections accessible to the token user.';

export const DRONECI_DESC = `Drone is a self-service Continuous Integration platform for busy development teams.`;

export const CIRCLECI_REPO_DESC =
  'List of repositories to ingest. Use the format "provider/owner/name". Leave blank to ingest all the repositories from collections accessible to the token user.';

export const MS_TEAMS_DESC =
  "Microsoft Teams is a proprietary business communication platform developed by Microsoft, as part of the Microsoft 365 family of products.";

export const HARNESS_ORGANIZATION_DESC =
  'Please use comma-separated names. For example, "Youtube, YouTubeTV".Note: The names you give are case-sensitive. Leave blank to ingest all the collections accessible to the token user.';

export const HARNESS_DESC = `Harness is a modern software delivery platform that allows engineers and DevOps to build, test, deploy, and verify software, on-demand.`;

export const HARNESS_PROJECT_DESC = `Please use comma-separated names. For example, "org/project, org2/project2". Note: The names you give are case-sensitive. Leave blank to ingest all the projects from collections accessible to the token user.
`;
