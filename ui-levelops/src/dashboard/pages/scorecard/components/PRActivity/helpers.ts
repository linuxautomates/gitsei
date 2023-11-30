import { cloneDeep } from "lodash";
import { PR_DATA_INDEX, emptyActivityList, PR_STATUS } from "shared-resources/charts/activity-table/helper";

export const DEV_PRODUCTIVITY_USER_PR_ACTIVITY = "dev_productivity_user_pr_activity";

export const transformActivityData = (inputData: any) => {
  const name = inputData.full_name || inputData.user_name || inputData.repo_id;
  const activityList = cloneDeep(emptyActivityList);

  inputData.stacks.forEach((stack: any) => {
    const day = stack.key;
    Object.values(PR_STATUS).forEach((status: string) => {
      activityList[status][day] = stack[PR_DATA_INDEX[status as PR_STATUS]] || 0;
      activityList[status].total += stack[PR_DATA_INDEX[status as PR_STATUS]] || 0;
    });
  });

  const data = Object.entries(activityList).reduce((acc: any, obj: any) => {
    acc.push({
      name,
      type: obj[0],
      ...obj[1]
    });
    return acc;
  }, []);

  return {
    [name]: data
  };
};

export const viewByOptions = [
  {
    label: "View By Repo",
    key: "repo_id"
  },
  {
    label: "View By Developer",
    key: "integration_user"
  }
];
