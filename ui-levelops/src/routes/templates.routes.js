import {
  AssessmentTemplatesEdit,
  AssessmentTemplatesList,
  KBCreateEdit,
  KBList,
  TemplatesAdd,
  TemplatesList
} from "../configurations/pages";
import { SmartTicketTemplateEdit, SmartTicketTemplateList } from "../smart-tickets/pages";
import { TemplateMenu } from "../templates/pages";
import { getBaseUrl, TEMPLATE_ROUTES } from "../constants/routePaths";
import { navigateToRoute } from "utils/routeUtils";
import { USER_ADMIN_ROLES } from "./helper/constants";

export function templateRoutes(hideAppSTT) {
  return [
    {
      name: "Templates",
      path: TEMPLATE_ROUTES._ROOT,
      layout: getBaseUrl(),
      rbac: USER_ADMIN_ROLES,
      component: TemplateMenu,
      collapse: false,
      id: "templates",
      label: "Templates",
      icon: "templatesSidebar",
      actions: {},
      items: [
        {
          label: "Templates",
          path: "",
          icon: "",
          description: "Template Pages Menu"
        },
        {
          // Assessment Templates List
          path: "/assessment-templates",
          rbac: USER_ADMIN_ROLES,
          description: "View and manage assessment templates",
          label: "Assessments Templates",
          hasAction: true,
          actionId: "add-questionnaire-template",
          actionRoute: "questions-page",
          actionLabel: "Add Assessment Template",
          buttonHandler: () => navigateToRoute(getBaseUrl(), TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.CREATE),
          dynamicHeader: true
        },
        {
          // Assessment Templates Edit
          path: "/assessment-templates/edit",
          name: "Edit Questionnaire",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // Assessment Templates Create
          path: "/assessment-templates/create",
          name: "Build Questionnaire",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // Issue Templates List
          path: "/issue-templates",
          rbac: USER_ADMIN_ROLES,
          description: "View and manage issue templates",
          label: "Issue Templates",
          hasAction: true,
          actionId: "edit-issue_template",
          actionRoute: "edit-issue-template",
          actionLabel: "Add Issue Template",
          hide: hideAppSTT,
          buttonHandler: () => navigateToRoute(getBaseUrl(), TEMPLATE_ROUTES.ISSUE_TEMPLATE.EDIT)
        },
        {
          // Issue Templates Edit/Create
          path: "/issue-templates/edit",
          name: "Smart Ticket Template Edit",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // Knowledge Bases List
          path: "/knowledge-bases",
          rbac: USER_ADMIN_ROLES,
          description: "View and manage knowledge base articles",
          label: "Knowledge Base",
          hasAction: true,
          actionId: "add-kb",
          actionRoute: "add-kb-page",
          actionLabel: "Add Knowledge Base",
          buttonHandler: () => navigateToRoute(getBaseUrl(), TEMPLATE_ROUTES.KB.CREATE)
        },
        {
          // Knowledge Bases Edit
          path: "/knowledge-bases/edit",
          name: "Edit KnowledgeBase",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // Knowledge Bases Create
          path: "/knowledge-bases/create",
          name: "Add KnowledgeBase",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // Communication Templates List
          path: "/communication-templates",
          rbac: USER_ADMIN_ROLES,
          description: "View and manage communication templates",
          label: "Templates",
          hasAction: true,
          actionId: "add-template",
          actionRoute: "add-template-page",
          actionLabel: "Add Template",
          buttonHandler: () => navigateToRoute(getBaseUrl(), TEMPLATE_ROUTES.COMMUNICATION_TEMPLATES.CREATE)
        },
        {
          // Communication Templates Create
          path: "/communication-templates/create",
          name: "Add Template",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          // Communication Templates Edit
          path: "/communication-templates/edit",
          name: "Edit Template",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        }
      ]
    },
    {
      // Assessment Templates List
      path: TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.LIST,
      layout: getBaseUrl(),
      name: "Assessment Templates",
      mini: "QL",
      component: AssessmentTemplatesList,
      rbac: USER_ADMIN_ROLES,
      id: "view-assessment_templates",
      description: "",
      label: "Assessments Templates",
      icon: "assessmentTemplates"
    },
    {
      // Assessment Templates Create
      path: TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.CREATE,
      layout: getBaseUrl(),
      name: "Build Questionnaire",
      mini: "Q",
      component: AssessmentTemplatesEdit,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      // Assessment Templates Edit
      path: TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.EDIT,
      layout: getBaseUrl(),
      name: "Edit Questionnaire",
      mini: "EQ",
      component: AssessmentTemplatesEdit,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      // Issue Templates List
      path: TEMPLATE_ROUTES.ISSUE_TEMPLATE.LIST,
      layout: getBaseUrl(),
      name: "Smart Ticket Template List",
      mini: "STT",
      component: SmartTicketTemplateList,
      rbac: USER_ADMIN_ROLES,
      hide: hideAppSTT,
      collapse: false,
      id: "view_issue_templates",
      label: "Issue Templates",
      icon: "templates"
    },
    {
      // Issue Templates Edit/Create
      path: TEMPLATE_ROUTES.ISSUE_TEMPLATE.EDIT,
      layout: getBaseUrl(),
      name: "Smart Ticket Template Edit",
      mini: "STT",
      component: SmartTicketTemplateEdit,
      rbac: USER_ADMIN_ROLES,
      invisible: true
    },
    {
      // Knowledge Bases List
      path: TEMPLATE_ROUTES.KB.LIST,
      layout: getBaseUrl(),
      name: "KnowledgeBase",
      mini: "KB",
      component: KBList,
      rbac: USER_ADMIN_ROLES,
      description: "",
      label: "Knowledge Base",
      icon: "knowledgeBase"
    },
    {
      // Knowledge Bases Create
      path: TEMPLATE_ROUTES.KB.CREATE,
      layout: getBaseUrl(),
      name: "Add KnowledgeBase",
      mini: "KB",
      component: KBCreateEdit,
      rbac: USER_ADMIN_ROLES,
      invisible: true
    },
    {
      // Knowledge Bases Edit
      path: TEMPLATE_ROUTES.KB.EDIT,
      layout: getBaseUrl(),
      name: "Edit KnowledgeBase",
      mini: "KB",
      component: KBCreateEdit,
      rbac: USER_ADMIN_ROLES,
      invisible: true
    },
    {
      // Communication Templates List
      path: TEMPLATE_ROUTES.COMMUNICATION_TEMPLATES.LIST,
      layout: getBaseUrl(),
      name: "Templates",
      mini: "TEMP",
      component: TemplatesList,
      rbac: USER_ADMIN_ROLES,
      id: "view-communication-templates",
      description: "",
      label: "Communication Templates",
      icon: "templates"
    },
    {
      // Communication Templates Create
      path: TEMPLATE_ROUTES.COMMUNICATION_TEMPLATES.CREATE,
      layout: getBaseUrl(),
      name: "Add Template",
      mini: "AT",
      component: TemplatesAdd,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      // Communication Templates Edit
      path: TEMPLATE_ROUTES.COMMUNICATION_TEMPLATES.EDIT,
      layout: getBaseUrl(),
      name: "Edit Template",
      mini: "AT",
      component: TemplatesAdd,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    }
  ];
}
