import { JENKINS_CICD_JOB_COUNT_CHILD_FILTER_KEY, JENKINS_CICD_JOB_COUNT_CHILD_FILTER_LABLE } from "dashboard/reports/jenkins/constants";
import { withDeleteAPIProps, WithSwitchFilterProps } from "../common/common-api-filter-props";

export const jenkinsJobApiFilterProps = (args: any) => ({
  withDelete: withDeleteAPIProps(args),
  withSwitch: WithSwitchFilterProps(args)
});

export const getChildKeys = (parentKey: string) => {
  let childKeys = JENKINS_CICD_JOB_COUNT_CHILD_FILTER_KEY.filter((data) => data.parnentKey === parentKey);
  return childKeys && childKeys.length > 0 ? childKeys[0].childKey : [];
}

export const getChildFilterLableName = (childKey: string, keyName?: string) => {
  let childKeysLable = JENKINS_CICD_JOB_COUNT_CHILD_FILTER_LABLE.filter((data) => data.childKey === childKey);
  return childKeysLable && childKeysLable.length > 0
    ? keyName === 'filterName'
      ? childKeysLable[0].filterName
      : childKeysLable[0].lableName
    : [];
}