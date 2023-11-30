export const mockFiltersData = [
  {
    status: [
      { key: "BACKLOG" },
      { key: "DONE" },
      { key: "IN PROGRESS" },
      { key: "IN REVIEW" },
      { key: "SELECTED FOR DEVELOPMENT" },
      { key: "TO DO" },
      { key: "UX AWAIT APPROVE" },
      { key: "UX BACKLOG" },
      { key: "UX WIREFRAME" },
      { key: "WONT DO" }
    ]
  },
  { priority: [{ key: "HIGH" }, { key: "HIGHEST" }, { key: "LOW" }, { key: "MEDIUM" }] },
  {
    issue_type: [
      { key: "BUG" },
      { key: "EPIC" },
      { key: "IMPROVEMENT" },
      { key: "NEW FEATURE" },
      { key: "STORY" },
      { key: "SUB-TASK" },
      { key: "TASK" }
    ]
  },
  {
    assignee: [
      { key: "Alexandru Arghiroiu" },
      { key: "Amit Zohar" },
      { key: "Andreea Cozonac" },
      { key: "Bethany Collins" },
      { key: "Chandra Yalangi" },
      { key: "Chinmay Joshi" },
      { key: "Harsh Jariwala" },
      { key: "Hemant Saini" },
      { key: "Ishan Srivastava" },
      { key: "Ivan Leon" },
      { key: "Kushagra Saxena" },
      { key: "Maxime Bellier" },
      { key: "Meghana Dwarakanath" },
      { key: "Megha Tamvada" },
      { key: "Nishant Doshi" },
      { key: "Piyush Mishra" },
      { key: "Prateek Joshi" },
      { key: "Rafay Shaukat" },
      { key: "Srinath Chandrashekhar" },
      { key: "Temoke Sanchez" },
      { key: "Test1 1" },
      { key: "Thanh Tran" },
      { key: "Tushar Raj" },
      { key: "_UNASSIGNED_" },
      { key: "Viraj Ajgaonkar" }
    ]
  },
  {
    project: [
      { key: "APSEC" },
      { key: "AUTO" },
      { key: "CGN" },
      { key: "LEV" },
      { key: "NP" },
      { key: "TEST2" },
      { key: "TM12" },
      { key: "TS" }
    ]
  },
  {
    component: [
      { key: "commons-levelops" },
      { key: "serverapi-levelops" },
      { key: "ingestion-levelops" },
      { key: "internalapi-levelops" },
      { key: "jenkins-plugin" },
      { key: "infrastructure" },
      { key: "runbooks" },
      { key: '"asdhahdhahaha ajahahahah"' },
      { key: "customer-requests" },
      { key: "provisioning" },
      { key: "ui-levelops" },
      { key: "ux" }
    ]
  },
  {
    label: [
      { key: "usability" },
      { key: "MVP2" },
      { key: "service-discovery" },
      { key: "regression" },
      { key: "contrastsecurity" },
      { key: "tools" },
      { key: "jira_escalated" },
      { key: "test" },
      { key: "k8s" },
      { key: "label2" },
      { key: "client" },
      { key: "containers" },
      { key: "label3" },
      { key: "test1" },
      { key: "abc" },
      { key: "RSA" },
      { key: "sast" },
      { key: "customer-request" },
      { key: "label1" },
      { key: "customfleet" },
      { key: "IaC" },
      { key: "production" },
      { key: "static" },
      { key: "test4" },
      { key: "mac" },
      { key: "plugins" },
      { key: "test3" },
      { key: "test2" },
      { key: "thoughtspot" },
      { key: "test123" },
      { key: "test-label" },
      { key: "intuit" },
      { key: "ux" },
      { key: "dast" }
    ]
  },
  {
    reporter: [
      { key: "Andreea Cozonac" },
      { key: "Chandra Yalangi" },
      { key: "Chinmay Joshi" },
      { key: "Harsh Jariwala" },
      { key: "Hemant Saini" },
      { key: "Ishan Srivastava" },
      { key: "Ivan Leon" },
      { key: "Kushagra Saxena" },
      { key: "Maxime Bellier" },
      { key: "Meghana Dwarakanath" },
      { key: "Megha Tamvada" },
      { key: "Nishant Doshi" },
      { key: "Piyush Mishra" },
      { key: "Prateek Joshi" },
      { key: "Rafay Shaukat" },
      { key: "Srinath Chandrashekhar" },
      { key: "Temoke Sanchez" },
      { key: "Thanh Tran" },
      { key: "Tushar Raj" },
      { key: "Viraj Ajgaonkar" }
    ]
  },
  { fix_version: [{ key: "MVP2" }, { key: "MVP1" }, { key: "Q1-2021" }, { key: "V1" }, { key: "Q4-2020" }] },
  { version: [{ key: "V1" }] }
];

export const mockCustomFiltersData = [
  {
    name: "sf_case_id",
    key: "customfield_10043",
    values: [{ key: "00001000" }, { key: "5003t00001ATWe3AAH" }, { key: "5003t00001ATWeAAAX" }]
  },
  {
    name: "CGN test labels",
    key: "customfield_10044",
    values: [{ key: "test" }, { key: "test1" }, { key: "test3" }, { key: "test2" }]
  },
  { name: "CGN test field", key: "customfield_10045", values: [{ key: "test_single_field" }, { key: "test_value" }] },
  {
    name: "CGN test multivalue field",
    key: "customfield_10046",
    values: [{ key: "test" }, { key: "testfield" }, { key: "test1" }, { key: "test2" }]
  },
  {
    name: "Story Points",
    key: "customfield_10030",
    values: [
      { key: "0.5" },
      { key: "1.0" },
      { key: "10000.0" },
      { key: "12.0" },
      { key: "1234.0" },
      { key: "2.0" },
      { key: "3.0" },
      { key: "42.0" },
      { key: "43.0" },
      { key: "5.0" },
      { key: "8.0" },
      { key: "9.0" },
      { key: "90.0" }
    ]
  }
];

export const mockGithubFiltersData = [
  {
    repo_id: [
      { key: "virajajgaonkar/leetcode", count: 3 },
      { key: "harsh-levelops/helloworld", count: 2 }
    ]
  },
  {
    creator: [
      { key: "Viraj Ajgaonkar", count: 3 },
      { key: "Harsh Jariwala", count: 2 }
    ]
  },
  {
    state: [
      { key: "MERGED", count: 4 },
      { key: "OPEN", count: 1 }
    ]
  },
  {
    branch: [
      { key: "jen", count: 3 },
      { key: "Harsh-Jariwala/readmemd-lev1613-1603150035714", count: 1 },
      { key: "testing", count: 1 }
    ]
  },
  {
    assignee: [
      { key: "Viraj Ajgaonkar", count: 3 },
      { key: "Harsh Jariwala", count: 1 }
    ]
  },
  {
    reviewer: [
      { key: "Viraj Ajgaonkar", count: 3 },
      { key: "Harsh Jariwala", count: 1 }
    ]
  },
  { label: [] }
];
