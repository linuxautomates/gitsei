// export const PREDICATES = {
//     code: {
//         type: ["infra","application"],
//         filepath:[],
//         code: "",
//         pr: {num_lines:""},
//         branch: "",
//         technology: [],
//         dependency_name: "",
//         dependency_version: ""
//     },
//     integration: {
//         type: "",
//         name: "",
//     },
//     documentation: {
//         type: "",
//         name: "",
//         title: "",
//         path: "",
//         technology: []
//     },
//     ticket: {
//         issue_type: [],
//         component: [],
//         label: "",
//         status: "",
//         fix_version: "",
//         assignee: "",
//         reporter: "",
//         technology: []
//     }
//
// };

export const PREDICATES = [
  // {
  //     columnField: "code.type",
  //     type: "text",
  //     suggestions: ["infra","application"]
  // },
  // {
  //     columnField: "code.commits",
  //     type: "number",
  //     suggestions: ["1","2","3","4","5"]
  // },
  // {
  //     columnField: "code.pr.size",
  //     type: "number",
  //     suggestions: ["100","200","300","500","1000","2000","5000","10000"]
  // },
  // {
  //     columnField: "code.branch",
  //     type: "text",
  //     suggestions: []
  // },
  // {
  //     columnField: "code.repo",
  //     type: "text",
  //     suggestions: ["levelops/ui-levelops","levelops/api-levelops","levelops/internalapi-levelops"]
  // },
  // {
  //     columnField: "code.filepath",
  //     type: "text",
  //     suggestions: []
  // },
  // {
  //     columnField: "code.technology",
  //     type: "text",
  //     suggestions: []
  // },
  // {
  //     columnField: "code.dependency",
  //     type: "text",
  //     suggestions: []
  // },
  // {
  //     columnField: "document.text",
  //     type: "text",
  //     suggestions: []
  // },
  // {
  //     columnField: "document.type",
  //     type: "text",
  //     suggestions: ["Confluence","Google Drive"]
  // },
  // {
  //     columnField: "document.title",
  //     type: "text",
  //     suggestions: []
  // },
  // {
  //     columnField: "document.path",
  //     type: "text",
  //     suggestions: []
  // },
  // {
  //     columnField: "document.technology",
  //     type: "text",
  //     suggestions: ["Java","Spring","Python","Terraform","MongoDB","MySQL","Cassandra","nginx"]
  // },
  // {
  //     columnField: "issue.text",
  //     type: "text",
  //     suggestions: []
  // },
  {
    columnField: "issue.type",
    type: "text",
    suggestions: ["Bug", "Epic", "Sub-task"]
  },
  {
    columnField: "issue.title",
    type: "text",
    suggestions: []
  },
  {
    columnField: "issue.status",
    type: "text",
    suggestions: ["Open", "Closed", "Done", "InProgress"]
  },
  {
    columnField: "issue.assignee",
    type: "text"
  },
  {
    columnField: "issue.reporter",
    type: "text"
  },
  {
    columnField: "issue.fix_version",
    type: "text"
  },
  // {
  //     columnField: "issue.technology",
  //     type: "text"
  // },
  {
    columnField: "issue.component",
    type: "text"
  },
  {
    columnField: "issue.label",
    type: "text"
  },
  {
    columnField: "issue.created_at",
    type: "text"
  },
  {
    columnField: "issue.updated_at",
    type: "text"
  },
  {
    columnField: "issue.project_key",
    type: "text"
  }
  // {
  //     columnField: "resource.text",
  //     type: "text"
  // },
  // {
  //     columnField: "resource.type",
  //     type: "text"
  // },
  // {
  //     columnField: "resource.tag",
  //     type: "text"
  // },
  // {
  //     columnField: "resource.environment",
  //     type: "text"
  // },
  // {
  //     columnField: "integration.name",
  //     type: "text"
  // },
  // {
  //     columnField: "integration.type",
  //     type: "text",
  //     suggestions: ["code","issue","document","infra"]
  // }
];

export const ISSUE_PREDICATES = [
  {
    columnField: "issue.type",
    type: "text",
    suggestions: ["Bug", "Epic", "Sub-task"]
  },
  {
    columnField: "issue.title",
    type: "text",
    suggestions: []
  },
  {
    columnField: "issue.status",
    type: "text",
    suggestions: ["Open", "Closed", "Done", "InProgress"]
  },
  {
    columnField: "issue.assignee",
    type: "text"
  },
  {
    columnField: "issue.reporter",
    type: "text"
  },
  {
    columnField: "issue.fix_version",
    type: "text"
  },
  {
    columnField: "issue.component",
    type: "text"
  },
  {
    columnField: "issue.label",
    type: "text"
  }
];

export const SUGGESTIONS = {};

// export const PREDICATES = {
//     code: [
//         {
//             columnField: "code.type",
//             type: "text",
//         },
//         {
//             columnField: "code.commits",
//             type: "number"
//         },
//         {
//             columnField: "code.pr.size",
//             type: "number"
//         },
//         {
//             columnField: "code.branch",
//             type: "text"
//         },
//         {
//             columnField: "code.repo",
//             type: "text"
//         },
//         {
//             columnField: "code.filepath",
//             type: "text"
//         },
//         {
//             columnField: "code.technology",
//             type: "text"
//         },
//         {
//             columnField: "code.dependency",
//             type: "text"
//         }
//     ],
//     document: [
//         {
//             columnField: "document.text",
//             type: "text"
//         },
//         {
//             columnField: "document.type",
//             type: "text"
//         },
//         {
//             columnField: "document.title",
//             type: "text"
//         },
//         {
//             columnField: "document.path",
//             type: "text"
//         },
//         {
//             columnField: "document.technology",
//             type: "text"
//         }
//     ],
//     issue: [
//         {
//             columnField: "issue.text",
//             type: "text"
//         },
//         {
//             columnField: "issue.type",
//             type: "text"
//         },
//         {
//             columnField: "issue.title",
//             type: "text"
//         },
//         {
//             columnField: "issue.status",
//             type: "text"
//         },
//         {
//             columnField: "issue.assignee",
//             type: "text"
//         },
//         {
//             columnField: "issue.technology",
//             type: "text"
//         },
//         {
//             columnField: "issue.component",
//             type: "text"
//         },
//         {
//             columnField: "issue.label",
//             type: "text"
//         }
//     ],
//     resource: [
//         {
//             columnField: "resource.text",
//             type: "text"
//         },
//         {
//             columnField: "resource.type",
//             type: "text"
//         },
//         {
//             columnField: "resource.tag",
//             type: "text"
//         },
//         {
//             columnField: "resource.environment",
//             type: "text"
//         }
//     ]
// };

export const OPERATORS = [">", "<", "=", "!=", ">=", "<=", "in", "nin", "NOT", "~"];

export const STRING_OPERATORS = ["=", "!=", "in", "nin", "NOT", "~", ">", "<"];

export const TABLE_OPERATORS = ["="];

export const PARTIAL_OPERATORS = ["~", ...TABLE_OPERATORS];
