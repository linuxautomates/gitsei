export const scoreRadars = [
  { dataKey: "A", theme: "blue" },
  { dataKey: "B", theme: "red" }
];

export const scoreData = [
  { subject: "Service", A: 120, B: 110 },
  { subject: "Access", A: 98, B: 130 },
  { subject: "Admin", A: 86, B: 130 },
  { subject: "Information", A: 99, B: 100 },
  { subject: "Data", A: 85, B: 90 },
  { subject: "Business", A: 65, B: 85 }
];

export const trendsData = [
  { name: "Page A", value: 4000 },
  { name: "Page B", value: 3000 },
  { name: "Page C", value: 2000 },
  { name: "Page D", value: 2780 },
  { name: "Page E", value: 1890 },
  { name: "Page F", value: 2390 },
  { name: "Page G", value: 3490 }
];

export const trendsAreas = [{ dataKey: "value", theme: "blue" }];

export const infrastructureIssuesData = [
  { name: "02/03", low: 40, medium: 24, high: 24 },
  { name: "02/10", low: 30, medium: 13, high: 22 },
  { name: "02/17", low: 20, medium: 18, high: 23 },
  { name: "02/24", low: 27, medium: 39, high: 20 }
];

export const infrastructureBars = [
  { name: "Low", dataKey: "low", fill: "#278CE6" },
  { name: "Medium", dataKey: "medium", fill: "#FBD64B" },
  { name: "High", dataKey: "high", fill: "#F4657C" }
];

export const vulnerabilityData = [
  { name: "02/03", new: 26, ignored: 24, resolved: 20 },
  { name: "02/10", new: 30, ignored: 11, resolved: 16 },
  { name: "02/17", new: 39, ignored: 24, resolved: 29 },
  { name: "02/24", new: 23, ignored: 12, resolved: 20 }
];

export const vulnerabilityBars = [
  { name: "New", dataKey: "new", fill: "#278CE6" },
  { name: "Ignored", dataKey: "ignored", fill: "#FBD64B" },
  { name: "Resolved", dataKey: "resolved", fill: "#F4657C" }
];

export const generalViolationsData = [
  {
    iconType: "security-scan",
    title: "SECURITY TRAINING",
    description: "Security training has not been completed in the last 90 days."
  },
  {
    iconType: "swap",
    title: "SINGLE SIGN ON",
    description: "Sign sign on is not enabled. Lorem ipsum dolor sit amet etiaud."
  }
];

export const currentIssuesData = [
  { name: "Story", value: 10232 },
  { name: "Improvement", value: 15232 },
  { name: "Task", value: 2320 },
  { name: "Sub-Task", value: 5420 },
  { name: "New Feature", value: 12302 },
  { name: "Bug", value: 1024 },
  { name: "Epic Data", value: 2048 }
];

export const gridData = [
  {
    header: "Planning",
    id: "13",
    securityViolations: {
      count: 31,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["aha"]
    },
    engineeringViolations: {
      count: 23,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["aha", "github"]
    }
  },
  {
    header: "Development",
    id: "14",
    securityViolations: {
      count: 26,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["jenkins"]
    },
    engineeringViolations: {
      count: 87,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    }
  },
  {
    header: "SAST",
    id: "15",
    securityViolations: {
      count: 10,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    },
    engineeringViolations: {
      count: 42,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    }
  },
  {
    header: "Build",
    id: "16",
    securityViolations: {
      count: 31,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    },
    engineeringViolations: {
      count: 23,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    }
  },
  {
    header: "QA",
    id: "17",
    securityViolations: {
      count: 26,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    },
    engineeringViolations: {
      count: 87,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    }
  },
  {
    header: "Infrastructure",
    id: "18",
    securityViolations: {
      count: 10,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    },
    engineeringViolations: {
      count: 42,
      description: "Stage Owner: jdoe@acme.io",
      icons: ["github"]
    }
  }
];
