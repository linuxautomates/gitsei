export const Policy = {
  id: undefined,
  name: "",
  description: "",
  severity: "MEDIUM",
  status: "ACTIVE",
  assignee_ids: [],
  lqls: [],
  assigning_process: "round_robin",
  team_ids: [],
  actions: {},
  communication: {}
};

export const PolicySeverity = ["LOW", "MEDIUM", "HIGH"];

export const PolicyAssigned = "round-robin";

export const PolicyStatus = ["ACTIVE", "INACTIVE"];
