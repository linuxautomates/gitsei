export const jenkinsRangeChoiceMapping: { [x: string]: string } = {
  // end_time: "jenkins_end_time"
};

export const jenkinsGithubRangeChoiceMapping: { [x: string]: string } = {
  // end_time: "jenkinsgithub_end_time"
};

export const jiraSalesforceZendeskCommonRangeChoiceMapping = (application: string) => ({
  issue_created_at: `${application}_issue_created_at`,
  issue_updated_at: `${application}_issue_updated_at`
});
