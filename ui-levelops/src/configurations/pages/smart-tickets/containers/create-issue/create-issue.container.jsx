import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { Form, Input, Typography } from "antd";
import queryString from "query-string";
import { AntButton, AntCol, AntModal, AntRow, AntSelect, AntText, AntCheckbox } from "shared-resources/components";
import {
  mapGenericToProps,
  mapQuizToProps,
  mapSectionsToProps,
  mapWorkitemsToProps,
  mapWorkspaceToProps
} from "reduxConfigs/maps/restapi";
import { SelectRestapi } from "shared-resources/helpers";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { GenericFormComponent } from "shared-resources/containers";
import { getSmartTicketFieldsSelector } from "reduxConfigs/selectors/restapiSelector";
import { Questions } from "assessments/components";
import { RestQuiz } from "classes/RestQuiz";
import { getData, getLoading, loadingStatus } from "utils/loadingUtils";

import "./create-issue.style.scss";
import { RestWorkItem } from "classes/RestWorkItem";

import LocalStoreService from "services/localStoreService";
import { DYNAMIC_MULTI_SELECT_TYPE, DYNAMIC_SINGLE_SELECT_TYPE } from "constants/fieldTypes";
import { RestTags } from "../../../../../classes/RestTags";
import { getError } from "../../../../../utils/loadingUtils";
import { TICKET_TYPES } from "constants/workitem";
import { AssigneeSelect } from "../../components";
import { getProductsBulkListSelector } from "reduxConfigs/selectors/restapiSelector";
import { isUndefined, get } from "lodash";
import { mapUserFormStateToProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { RestUsers } from "../../../../../classes/RestUsers";
import { sanitizeHtmlAndLink } from "../../helper";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";

const { Title } = Typography;

export class CreateIssueContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.modalScrollRef = React.createRef();
    this.state = {
      title: "",
      template_id: null,
      template_name: undefined,
      template_has_questionnaire: false,
      template_fields: [],
      template_loading: false,
      isVisible: props.isVisible,
      sections: [],
      currentSectionIndex: null,
      questionsToUpdate: {},
      deleting_workitem: false,
      assignees: null,
      tags: null,
      tag_ids: [],
      status: {},
      product: {},
      product_name: undefined,
      product_loading: false,
      attachments: [],
      loading_questionaires: false,
      loading_questionaires_list: false,
      questionnaires: [],
      quiz_updating_id: null,
      quiz_submit_loading: false,
      creating_workitem: false,
      workitem_created: false,
      values: {},
      ticket_type: RestWorkItem.DEFAULT_TICKET_TYPE,
      user_has_selected_assignee: false,
      skip_questionnaire: false,
      notify: true,
      default_fields: {},
      default_assignee_loading: false,
      product_owner: undefined,
      set_template: false,
      template_select: {},
      showCreateAssignee: false,
      createAssigneeLoading: false,
      newUserSelectionId: undefined,
      newUserLoading: false,
      refreshAssigneeList: 0
    };

    this.props.formInitialize("user_form", {});
  }

  componentDidMount() {
    // get your default product here and save it
  }

  componentDidUpdate(prevProps, prevState) {
    if (prevState.currentSectionIndex !== this.state.currentSectionIndex) {
      this.modalScrollRef.current.scrollTo(0, 0);
    }
  }

  componentWillUnmount() {
    this.props.restapiClear("workitem", "create", "-1");
    this.props.restapiClear("workitem", "update", "-1");
    this.props.restapiClear("workitem", "delete", "-1");
    this.props.restapiClear("quiz", "get", "-1");
    if (this.props.workitem.id) {
      this.props.restapiClear("quiz", "list", this.props.workitem.id);
    }

    this.props.restapiClear("tags", "getOrCreate", "-1");
    this.props.restapiClear("users", "getOrCreate", "-1");
    this.props.restapiClear("users", "create", "-1");
  }

  static getDerivedStateFromProps(props, prevState) {
    const {
      loading_questionaires,
      loading_questionaires_list,
      questionnaires,
      quiz_updating_id,
      currentSectionIndex,
      sections,
      creating_workitem,
      template_has_questionnaire,
      creating_tags,
      creating_users,
      tag_ids,
      product,
      product_name,
      product_loading,
      template_id,
      template_name,
      template_select,
      template_loading,
      set_template,
      default_product_loading,
      skip_questionnaire,
      default_template_loading,
      user_has_selected_assignee,
      default_fields,
      template_fields,
      default_assignee_loading,
      createAssigneeLoading,
      newUserLoading,
      newUserSelectionId
    } = prevState;
    const { issue_template_name, project_name, new: newIssue } = queryString.parse(props.location?.search || {});

    if (newIssue && template_name !== issue_template_name && !template_loading) {
      props.genericList(
        "ticket_templates",
        "list",
        { filter: { partial: { name: issue_template_name } } },
        null,
        "template_name"
      );
      return {
        ...prevState,
        template_name: issue_template_name,
        template_loading: true
      };
    }

    if (newIssue && product_name !== project_name && !product_loading) {
      props.genericList(
        "workspace",
        "list",
        { filter: { partial: { name: { starts: project_name } } } },
        null,
        "project_name"
      );
      return {
        ...prevState,
        product_name: project_name,
        product_loading: true
      };
    }

    if (newIssue && template_loading) {
      const loading = getLoading(props.rest_api, "ticket_templates", "list", "template_name");
      const error = getError(props.rest_api, "ticket_templates", "list", "template_name");

      if (!loading && !error) {
        const templates = getData(props.rest_api, "ticket_templates", "list", "template_name")?.records;
        if (templates.length > 0) {
          const template = templates[0];
          const template_has_questionnaire = !!template.questionnaire_templates.length;
          const template_fields = template.ticket_fields.map(field => ({
            ...field,
            key: field.id,
            label: field.key
          }));

          return {
            ...prevState,
            template_loading: false,
            default_template_loading: false,
            template_id: template.id,
            template_select: { value: template.id, label: template.name, key: template.id },
            template_fields,
            template_has_questionnaire,
            default_fields: template.default_fields || {
              summary: true,
              assignee: true,
              description: true,
              tags: true,
              type: true,
              attachments: true
            }
          };
        }
        return {
          ...prevState,
          template_loading: false
        };
      }
    }

    if (newIssue && product_loading) {
      const loading = getLoading(props.rest_api, "workspace", "list", "project_name");
      const error = getError(props.rest_api, "workspace", "list", "project_name");

      if (!loading && !error) {
        const products = getData(props.rest_api, "workspace", "list", "project_name")?.records;
        if (products.length > 0) {
          const product = products[0];
          if (!user_has_selected_assignee && product.owner_id !== undefined) {
            const loading = getLoading(props.rest_api, "users", "get", product.owner_id);
            if (loading) {
              props.usersGet(product.owner_id);
            }
          }
          return {
            ...prevState,
            product_loading: false,
            default_product_loading: false,
            default_assignee_loading: product.owner_id !== undefined,
            product: { label: product.name, key: product.id },
            product_owner: product.owner_id
          };
        }
        return {
          ...prevState,
          product_loading: false
        };
      }
    }

    if (template_id === null && default_template_loading !== true && !set_template && !template_loading) {
      const loading = getLoading(props.rest_api, "ticket_templates", "list", "default");
      if (loading) {
        props.genericList("ticket_templates", "list", { filter: { default: true } }, null, "default");
      }
      return {
        ...prevState,
        default_template_loading: true
      };
    }
    if (default_template_loading) {
      const loading = getLoading(props.rest_api, "ticket_templates", "list", "default");
      const error = getError(props.rest_api, "ticket_templates", "list", "default");

      if (!loading && !error) {
        const templates = getData(props.rest_api, "ticket_templates", "list", "default").records;
        if (templates.length > 0) {
          const template = templates[0];
          const template_has_questionnaire = !!template.questionnaire_templates.length;
          const template_fields = template.ticket_fields.map(field => ({
            ...field,
            key: field.id,
            label: field.key
          }));

          return {
            ...prevState,
            default_template_loading: false,
            template_id: template.id,
            template_select: { value: template.id, label: template.name, key: template.id },
            template_fields,
            template_has_questionnaire,
            default_fields: template.default_fields || {
              summary: true,
              assignee: true,
              description: true,
              tags: true,
              type: true,
              attachments: true
            }
          };
        }
      }
    }

    if ((product === undefined || product.key === undefined) && default_product_loading !== true && !product_loading) {
      if (props.product !== undefined) {
        props.workspaceGet(props.product);
        return {
          ...prevState,
          //product: props.product,
          default_product_loading: true
        };
      } else {
        const loading = getLoading(props.rest_api, "workspace", "list", "default");
        if (loading) {
          props.genericList("workspace", "list", { filter: { bootstrapped: true } }, null, "default");
        }
        return {
          ...prevState,
          default_product_loading: true
        };
      }
    }
    if (default_product_loading) {
      const method = props.product ? "get" : "list";
      const id = props.product ? props.product : "default";
      const loading = getLoading(props.rest_api, "workspace", method, id);
      const error = getError(props.rest_api, "workspace", method, id);

      if (!loading && !error) {
        const products =
          method === "list"
            ? getData(props.rest_api, "workspace", "list", "default").records || []
            : [getData(props.rest_api, "workspace", method, id)];
        if (products.length > 0) {
          const product = products[0];
          if (!user_has_selected_assignee && product.owner_id !== undefined) {
            const loading = getLoading(props.rest_api, "users", "get", product.owner_id);
            if (loading) {
              props.usersGet(product.owner_id);
            }
          }
          // TODO Use this when BE Changes
          // return {
          //   ...prevState,
          //   default_product_loading: false,
          //   product: { label: product.name, key: product.id },
          //   assignees
          // };

          return {
            ...prevState,
            default_product_loading: false,
            default_assignee_loading: product.owner_id !== undefined,
            product: { label: product.name, key: product.id },
            product_owner: product.owner_id
          };
        }
      }
    }

    if (default_assignee_loading || newUserLoading) {
      let id = prevState.product_owner;
      if (newUserLoading) {
        id = newUserSelectionId;
      }
      const loading = getLoading(props.rest_api, "users", "get", id);
      const error = getError(props.rest_api, "users", "get", id);
      if (!loading && !error) {
        const user = getData(props.rest_api, "users", "get", id);
        return {
          ...prevState,
          default_assignee_loading: false,
          newUserLoading: false,
          newUserSelectionId: undefined,
          assignees: newUserLoading
            ? [...prevState.assignees, { key: user.id, label: user.email }]
            : [{ key: user.id, label: user.email }]
        };
      }
    }

    if (!props.isVisible && props.isVisible !== prevState.isVisible) {
      // Reset state.
      return {
        creating_workitem: false,
        workitem_created: false,
        template_id: template_id,
        template_has_questionnaire: template_has_questionnaire,
        values: null,
        isVisible: props.isVisible,
        sections: [],
        description: "",
        title: null,
        template_fields: template_fields,
        questionsToUpdate: {},
        deleting_workitem: false,
        assignees: [],
        tags: [],
        tag_ids: [],
        status: {},
        product: product,
        attachments: [],
        currentSectionIndex: null,
        loading_questionaires: false,
        loading_questionaires_list: false,
        questionnaires: [],
        quiz_updating_id: null,
        ticket_type: RestWorkItem.DEFAULT_TICKET_TYPE,
        user_has_selected_assignee: false,
        skip_questionnaire: false,
        notify: true,
        default_fields: default_fields,
        default_product_loading: false,
        default_assignee_loading: false,
        product_owner: undefined,
        set_template: false,
        template_select: template_select,
        creating_users: false
      };
    }

    if (props.isVisible !== prevState.isVisible) {
      return {
        isVisible: props.isVisible
      };
    }

    if (creating_tags || creating_users) {
      let tagsLoading = creating_tags;
      let usersLoading = creating_users;
      const payload = CreateIssueContainer.getData(props, prevState);
      if (creating_tags) {
        const { loading, error } = loadingStatus(props.rest_api, "tags", "getOrCreate", 0);
        if (!loading && !error) {
          const newtags = props.rest_api.tags.getOrCreate[0].data;
          payload.tag_ids = newtags.map(t => t.id).concat(tag_ids);
          tagsLoading = false;
        }
      }
      if (creating_users) {
        const { loading, error } = loadingStatus(props.rest_api, "users", "getOrCreate", 0);
        if (!loading && !error) {
          const newUsers = get(props.rest_api, ["users", "getOrCreate", "0", "data"], []);
          payload.assignees = payload.assignees
            .filter(assignee => !assignee.user_id.includes("create:"))
            .concat(newUsers.map(user => ({ user_id: user.id, user_email: user.email })));
          usersLoading = false;
        }
      }

      if (!tagsLoading && !usersLoading) {
        props.workItemCreate(new RestWorkItem(payload));
        return {
          creating_workitem: true,
          creating_tags: false,
          creating_users: false
        };
      }
    }

    if (creating_workitem) {
      const { loading, error } = loadingStatus(props.rest_api, "workitem", "create", 0);
      if (!loading) {
        if (error) {
          return {
            creating_workitem: false
          };
        }
        if (!template_has_questionnaire || skip_questionnaire) {
          props.onSuccessEvent(props.workitem.vanity_id);
          return null;
        }
        props.quizList(
          {
            filter: {
              work_item_ids: [props.rest_api.workitem.create[0].data.id],
              main: true
            }
          },
          props.rest_api.workitem.create[0].data.id
        );
        return {
          creating_workitem: false,
          workitem_created: true,
          loading_questionaires: true
        };
      }
    }

    if (loading_questionaires) {
      const { loading, error } = loadingStatus(props.rest_api, "quiz", "list", props.workitem.id);
      if (!loading) {
        if (!error) {
          const questionnaires = getData(props.rest_api, "quiz", "list", props.workitem.id).records;
          // eslint-disable-next-line array-callback-return
          questionnaires.map(questionnaire => {
            props.quizGet(questionnaire.id);
          });
          return {
            questionnaires,
            loading_questionaires: false,
            loading_questionaires_list: true
          };
        }
        return {
          loading_questionaires: false
        };
      }
    }

    if (loading_questionaires_list) {
      let loadedAll = true;
      let failed = false;
      // eslint-disable-next-line array-callback-return
      questionnaires.map(questionnaire => {
        const { loading, error } = loadingStatus(props.rest_api, "quiz", "get", questionnaire.id);
        if (loading) {
          loadedAll = false;
        } else if (error) {
          failed = true;
        }
      });
      if (failed) {
        return {
          loading_questionaires_list: false
        };
      }
      if (loadedAll) {
        const completeQuestionnaires = [];
        const sections = questionnaires.reduce((carry, questionnaire) => {
          const data = getData(props.rest_api, "quiz", "get", questionnaire.id);
          // eslint-disable-next-line array-callback-return
          data.sections.map(section => {
            carry.push({
              ...section,
              questionnaire_id: questionnaire.id
            });
          });
          const quiz = new RestQuiz(data);
          completeQuestionnaires.push(quiz);
          return carry;
        }, []);
        return {
          sections,
          questionnaires: completeQuestionnaires,
          currentSectionIndex: 0,
          loading_questionaires_list: false
        };
      }
    }

    if (prevState.quiz_files_uploading) {
      let fileUploading = false;
      const quiz = questionnaires.filter(questionnaire => questionnaire.id === quiz_updating_id)[0];
      prevState.file_uploads.forEach(file => {
        const { loading, error } = loadingStatus(props.rest_api, "quiz", "upload", file.upload_id);
        if (!loading && !error) {
          const fileId = props.rest_api.quiz.upload[file.upload_id].data.id;
          for (let i = 0; i < quiz.section_responses.length; i++) {
            if (file.section_id.toString() === quiz.section_responses[i].section_id.toString()) {
              for (let j = 0; j < quiz.section_responses[i].answers.length; j++) {
                if (quiz.section_responses[i].answers[j].question_id.toString() === file.question_id.toString()) {
                  const responseIndex = quiz.section_responses[i].answers[j].responses.findIndex(
                    _response => _response.upload_id === file.upload_id
                  );
                  if (responseIndex !== -1) {
                    quiz.section_responses[i].answers[j].responses[responseIndex].value = fileId;
                    quiz.section_responses[i].answers[j].responses[responseIndex].upload = false;
                    quiz.section_responses[i].answers[j].upload = false;
                  }

                  break;
                }
              }
              break;
            }
          }
        } else {
          fileUploading = true;
        }
      });
      if (!fileUploading) {
        return {
          questionnaires: questionnaires.map(questionnaire => {
            if (quiz.id === questionnaire.id) {
              return new RestQuiz({ ...quiz.json() });
            }
            return questionnaire;
          }),
          quiz_files_uploading: false
        };
      }
    }

    if (prevState.quiz_submit_loading) {
      const { loading, error } = loadingStatus(props.rest_api, "quiz", "update", quiz_updating_id);
      if (!loading && !error) {
        const quizData = getData(props.rest_api, "quiz", "update", quiz_updating_id);
        const quiz = questionnaires.find(questionnaire => questionnaire.id === quiz_updating_id);
        if (quiz) {
          console.log(quiz.generation);
          console.log(quizData.generation);
          quiz.generation = quizData.generation;
        }

        return {
          quiz_updating_id: null,
          quiz_submit_loading: false,
          currentSectionIndex: currentSectionIndex + 1,
          questionnaires: questionnaires
        };
      }
    }

    if (prevState.deleting_workitem) {
      const { loading } = getLoading(props.rest_api, "workitem", "delete", prevState.template_id);
      if (!loading) {
        props.onCancelEvent();
        return {
          deleting_workitem: false
        };
      }
    }

    if (createAssigneeLoading) {
      if (!getLoading(props.rest_api, "users", "create", "0") && !getError(props.rest_api, "users", "create", "0")) {
        const id = get(props.rest_api, ["users", "create", "0", "data", "id"], {});
        props.restapiClear("users", "create", "-1");
        props.formClear("user_form");

        if (id) {
          props.usersGet(id);

          return {
            ...prevState,
            createAssigneeLoading: false,
            showCreateAssignee: false,
            newUserSelectionId: id,
            newUserLoading: true,
            refreshAssigneeList: prevState.refreshAssigneeList + 1
          };
        }

        return {
          ...prevState,
          createAssigneeLoading: false,
          showCreateAssignee: false
        };
      }
    }
    return null;
  }

  static getData = (props, state) => {
    const localStoreService = new LocalStoreService();
    const { assignees, status, product, title, template_id, tag_ids, description, ticket_type, notify } = state;
    const payload = {
      title,
      description,
      assignees: assignees
        ? assignees.map(assignee => ({
            user_id: assignee.key,
            user_email: assignee.label
          }))
        : undefined,
      state_id: status ? status.key : undefined,
      status: status ? status.label : undefined,
      product_id: product ? product.key : undefined,
      type: "MANUAL",
      parent_id: props.parent_id,
      reporter: localStoreService.getUserEmail(),
      notify: notify,
      tag_ids,
      ticket_type
    };
    if (template_id) {
      const { template_fields, values } = state;
      const ticket_fields = template_fields.map(field => {
        let val = [];
        if (field.label.toLowerCase() === "reporter") {
          val = [
            {
              value: localStoreService.getUserEmail(),
              type: "string"
            }
          ];
        } else {
          val =
            values[field.id] && values[field.id].value
              ? values[field.id].value.map(v => {
                  let value = v;
                  if (field.type === DYNAMIC_SINGLE_SELECT_TYPE || field.type === DYNAMIC_MULTI_SELECT_TYPE) {
                    value = v.key;
                  }
                  return {
                    value,
                    type: field.type === "date" ? "int" : "string"
                  };
                })
              : [];
        }
        field.values = val;
        field.key = field.label;
        return field;
      });
      payload.ticket_template_id = template_id;
      payload.ticket_data_values = ticket_fields.map(ticket_field => ({
        ...ticket_field,
        ticket_field_id: ticket_field.id,
        id: undefined
      }));
      // TBD
      // payload.attachments =  attachments.map(({ response }) => ({
      //   file_id: response.id,
      //   uploaded_on: moment().unix()
      // }))
    }
    return payload;
  };

  handleCreateAssigneeCancel = () => {
    this.setState(state => ({ showCreateAssignee: !state.showCreateAssignee }));
    this.props.formClear("user_form");
  };

  handleCreateAssigneeSubmit = () => {
    this.setState(
      {
        createAssigneeLoading: true
      },
      () => {
        let user = new RestUsers(this.props.user_form);
        this.props.usersCreate(user);
      }
    );
  };

  files_uploading = () => {
    const file_uploads = [];
    this.activeQuiz.section_responses.forEach(answer => {
      answer.answers.forEach(assertion => {
        if (assertion.answered && assertion.responses && assertion.responses.length > 0) {
          assertion.responses.forEach((response, index) => {
            if (response.hasOwnProperty("type") && response.type === "file" && response.upload === true) {
              this.props.quizFileUpload(`${this.activeQuiz.id}:${assertion.question_id}:${index}`, response.value);
              file_uploads.push({
                question_id: assertion.question_id,
                file: response.value,
                section_id: answer.section_id,
                questionnaire_id: this.activeQuiz.id,
                upload_id: `${this.activeQuiz.id}:${assertion.question_id}:${index}`
              });
            }
          });
        }
      });
    });
    return file_uploads;
  };

  getFileLoading = id => {
    const data = get(this.props.rest_api, ["quiz", "upload", id], undefined);
    if (data) {
      return data.loading;
    }
    return false;
  };

  handleNext = () => {
    this.props.quizUpdate(this.activeQuiz.id, this.activeQuiz);
    if (this.isLastSection) {
      this.props.onSuccessEvent(this.props.workitem.vanity_id);
      return;
    }
    this.setState({
      quiz_updating_id: this.activeQuiz.id,
      quiz_submit_loading: true
    });
  };

  handleBack = () => {
    const { currentSectionIndex } = this.state;
    if (currentSectionIndex) {
      this.setState({
        currentSectionIndex: currentSectionIndex - 1
      });
    }
  };

  createWorkItem = () => {
    this.props.restapiClear("workitem", "create", "-1");
    const { tags, assignees } = this.state;
    const newUsers = (assignees || [])
      .filter(assignee => assignee.key.includes("create:"))
      .map(assignee => assignee.key);
    if ((tags && tags.length > 0) || newUsers.length > 0) {
      const { newTags, existingTags } = RestTags.getNewAndExistingTags(tags);

      if (newTags.length > 0 || newUsers.length > 0) {
        const tagsToCreate = newTags.map(t => t.key);
        const tag_ids = existingTags.map(t => t.key);
        this.setState(
          {
            creating_tags: newTags.length > 0,
            creating_users: newUsers.length > 0,
            tag_ids
          },
          () => {
            if (newTags.length > 0) {
              this.props.tagsGetOrCreate(tagsToCreate);
            }
            if (newUsers.length > 0) {
              this.props.usersGetOrCreate(newUsers);
            }
          }
        );
        return;
      }
    }
    const tag_ids = tags ? tags.map(t => t.key) : [];
    const payload = CreateIssueContainer.getData(this.props, this.state);
    payload.tag_ids = tag_ids;
    this.setState(
      {
        creating_workitem: true
      },
      () => {
        this.props.workItemCreate(new RestWorkItem(payload));
      }
    );
  };

  handleCancel = () => {
    const { workitem } = this.props;
    if (!workitem || !workitem.id) {
      this.props.onCancelEvent();
      return;
    }
    this.props.workItemDelete(workitem.id);
    this.setState({ deleting_workitem: true });
  };

  downloadFile = (e, fileId, fileName) => {
    e.preventDefault();
    let downloadIds = this.state.file_download_ids || [];
    downloadIds.push({ file_id: fileId, file_name: fileName });
    this.setState(
      {
        file_downloading: true,
        file_download_ids: downloadIds
      },
      () => this.props.filesGet(fileId, fileName)
    );
  };

  handleQuizChange = quiz => {
    const filesUploading = this.files_uploading();
    this.setState(state => {
      const { questionnaires } = state;
      return {
        questionnaires: questionnaires.map(questionnaire => {
          if (quiz.id === questionnaire.id) {
            return new RestQuiz({
              ...quiz.json(),
              answered_questions: quiz.section_responses.reduce((total, section) => {
                total = total + section.answers.filter(answer => answer.answered === true).length;
                return total;
              }, 0)
            });
          }
          return questionnaire;
        }),
        file_uploads: filesUploading,
        quiz_files_uploading: filesUploading.length > 0,
        quiz_updating_id: this.activeQuiz.id
      };
    });
  };

  handleAssigneeChange = options => {
    console.log(options);
    options = options ? [...options] : options;
    this.setState({ assignees: options, user_has_selected_assignee: true });
  };

  handleTagsChange = tags => {
    this.setState({ tags });
  };

  handleAttachmentsChange = files => {
    const remainingFiles = files.filter(a => a.status !== "removed");
    if (files.length - remainingFiles.length > 0) {
      this.setState({ attachments: remainingFiles });
      return;
    }
    this.setState({ attachments: files });
  };

  handleProductChange = option => {
    const availableProducts = getData(this.props.rest_api, "workspace", "list", "0").records || [];
    const selectedProducts = availableProducts.filter(p => p.id === option.key);
    if (selectedProducts.length > 0) {
      const product = selectedProducts[0];
      this.setState(
        {
          product: option,
          default_assignee_loading: !this.state.user_has_selected_assignee && product.owner_id !== undefined,
          product_owner: product.owner_id
        },
        () => {
          if (!this.state.user_has_selected_assignee && product.owner_id !== undefined) {
            this.props.usersGet(product.owner_id);
          }
        }
      );
    }
    // TODO Use this when BE Changes
    // this.setState({ product: option, assignees });
  };

  handleTemplateChange = option => {
    const selectedTemplateId = option ? option.key : null;
    let template_fields = [];
    let hasQuestionnaire = false;
    let default_fields = {};
    if (selectedTemplateId) {
      const templates = this.ticketTemplates.filter(template => template.id === selectedTemplateId);
      if (templates.length > 0) {
        hasQuestionnaire = !!templates[0].questionnaire_templates.length;
        template_fields = templates[0].ticket_fields.map(field => ({
          ...field,
          key: field.id,
          label: field.key
        }));
        default_fields = templates[0].default_fields || {
          summary: true,
          assignee: true,
          description: true,
          tags: true,
          type: true,
          attachments: true
        };
      }
    }
    this.setState({
      template_id: selectedTemplateId,
      template_select: option,
      set_template: true,
      template_has_questionnaire: hasQuestionnaire,
      sections: [],
      currentSectionIndex: null,
      activeSection: null,
      questionnaire_id: null,
      template_fields,
      default_fields
    });
  };

  handleTicketTypeChange = option => {
    this.setState({
      ticket_type: option
    });
  };

  handleFieldValuesChange = values => {
    this.setState({
      values: {
        ...this.state.values,
        ...values
      }
    });
  };

  get hasQuestionnaireTemplates() {
    return (
      this.selectedTemplate &&
      this.selectedTemplate.questionnaire_templates &&
      this.selectedTemplate.questionnaire_templates.length > 0
    );
  }

  get assignee() {
    return (
      <Form.Item label="Assignee" colon={false}>
        <AssigneeSelect
          placeholder="AUTOMATIC"
          assignees={this.state.assignees || null}
          onAssigneesChange={this.handleAssigneeChange}
          user_form={this.props.user_form}
          formUpdateField={this.props.formUpdateField}
          showCreateAssignee={this.state.showCreateAssignee}
          createAssigneeLoading={this.state.createAssigneeLoading}
          handleCreateAssigneeCancel={this.handleCreateAssigneeCancel}
          handleCreateAssigneeSubmit={this.handleCreateAssigneeSubmit}
          newUserSelectionId={this.state.newUserSelectionId}
          refreshList={this.state.refreshAssigneeList}
          allowCreateAssignee={this.props.allowCreateAssignee}
        />
      </Form.Item>
    );
  }

  // TODO no attachments on create currently. Need to change that
  get defaultFields() {
    const { title, tags, description, default_fields } = this.state;
    const ls = new LocalStoreService();
    const rbac = ls.getUserRbac();
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol span={24}>
          <Form layout="vertical">
            <Form.Item label="Summary" required colon={false}>
              <Input
                value={title}
                onChange={event => {
                  this.setState({
                    title: sanitizeHtmlAndLink(event.target.value)
                  });
                }}
              />
            </Form.Item>
            {!isUndefined(default_fields.description) && default_fields.description === false ? null : (
              <Form.Item label={"Description"} colon={false}>
                <Input.TextArea
                  value={description}
                  onChange={event => {
                    this.setState({
                      description: event.target.value
                    });
                  }}
                />
              </Form.Item>
            )}
            {this.assignee}
            {!isUndefined(default_fields.tags) && default_fields.tags === false ? null : (
              <Form.Item label="Tags" colon={false} key="tags">
                <SelectRestapi
                  value={tags || null}
                  placeholder=""
                  mode="multiple"
                  labelInValue
                  uri="tags"
                  rest_api={this.props.rest_api}
                  fetchData={this.props.tagsList}
                  createOption={getRBACPermission(PermeableMetrics.ISSUE_CREATE_TAG_CREATION)}
                  searchField="name"
                  onChange={this.handleTagsChange}
                />
              </Form.Item>
            )}
            {!isUndefined(default_fields.type) && default_fields.type === false ? null : (
              <Form.Item label="Ticket Type" colon={false}>
                <AntSelect
                  options={TICKET_TYPES}
                  onChange={this.handleTicketTypeChange}
                  value={this.state.ticket_type}
                />
              </Form.Item>
            )}
            <Form.Item label={"Issue Template"} colon={false}>
              <SelectRestapi
                style={{ width: "100%" }}
                uri="ticket_templates"
                fetchData={this.props.smartTicketTemplatesList}
                rest_api={this.props.rest_api}
                searchField="name"
                uuid={"select-template"}
                mode={"single"}
                labelInValue={true}
                onChange={this.handleTemplateChange}
                allowClear={false}
                value={this.state.template_select || {}}
                //value={this.state.template_id || ""}
                placeholder="Template"
                filterOptionMethod={option => {
                  return option.enabled;
                }}
              />
            </Form.Item>
          </Form>
        </AntCol>
      </AntRow>
    );
  }

  get selectedTemplate() {
    const { template_id } = this.state;
    const templates = this.ticketTemplates.filter(template => template.id === template_id);
    if (templates.length === 0) {
      return null;
    }
    return templates[0];
  }

  get ticketTemplates() {
    const { loading, error } = loadingStatus(this.props.rest_api, "ticket_templates", "list", "select-template");
    if (loading || error) {
      return [];
    }
    return this.props.rest_api.ticket_templates.list["select-template"].data.records;
  }

  get workitemFields() {
    return this.state.template_fields.reduce((fields, field) => {
      if (field.label.toLowerCase() !== "reporter") {
        fields.push({
          ...field,
          values: this.state.values[field.id],
          label: field.display_name
        });
      }
      return fields;
    }, []);
  }

  get genericFields() {
    if (!this.state.template_id) {
      return null;
    }
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol span={24}>
          <GenericFormComponent
            desc=""
            layout="vertical"
            elements={this.workitemFields}
            onChange={this.handleFieldValuesChange}
          />
        </AntCol>
      </AntRow>
    );
  }

  get isFirstSection() {
    const { currentSectionIndex } = this.state;
    return currentSectionIndex === 0;
  }

  get isLastSection() {
    const { currentSectionIndex, sections } = this.state;
    return sections.length === currentSectionIndex + 1;
  }

  get activeSection() {
    const { currentSectionIndex, sections } = this.state;
    if (sections.length > currentSectionIndex) {
      return sections[currentSectionIndex];
    }
    return null;
  }

  get activeQuiz() {
    if (!this.activeSection) {
      return null;
    }
    const { questionnaires } = this.state;
    return questionnaires.filter(questionnaire => questionnaire.id === this.activeSection.questionnaire_id)[0];
  }

  get creatingTicket() {
    return this.state.creating_workitem;
  }

  get showQuestionnaire() {
    const { template_has_questionnaire, workitem_created, skip_questionnaire } = this.state;
    return workitem_created && template_has_questionnaire && !this.quizLoading && !skip_questionnaire;
  }

  get quizLoading() {
    const { loading_questionaires_list, loading_questionaires } = this.state;
    return loading_questionaires || loading_questionaires_list;
  }

  get quizSubmitting() {
    const { quiz_files_uploading, quiz_submit_loading } = this.state;
    return quiz_files_uploading || quiz_submit_loading;
  }

  get createDisabled() {
    const {
      template_id,
      title,
      product,
      template_fields,
      values,
      template_has_questionnaire,
      default_product_loading,
      default_assignee_loading,
      showCreateAssignee,
      createAssigneeLoading
    } = this.state;
    let inValid = !product.key || !title || default_product_loading || default_assignee_loading;
    if (template_id) {
      template_fields.forEach(field => {
        if (field.required === true) {
          if (!values[field.id] || !values[field.id].value || values[field.id].value.length === 0) {
            inValid = true;
            return inValid;
          }
        }
      });
    }
    if (!template_id) {
      return inValid || this.creatingTicket;
    }
    return (
      (template_has_questionnaire && this.quizLoading) ||
      inValid ||
      this.creatingTicket ||
      showCreateAssignee ||
      createAssigneeLoading
    );
  }

  get nextDisabled() {
    return this.quizSubmitting;
  }

  get actionBtn() {
    if (this.showQuestionnaire) {
      const { showCreateAssignee, createAssigneeLoading } = this.state;
      return (
        <>
          {!this.isFirstSection && !this.isLastSection && (
            <AntButton type="ghost" onClick={this.handleBack} disabled={this.nextDisabled}>
              Back
            </AntButton>
          )}
          <AntButton
            type="primary"
            onClick={this.handleNext}
            disabled={this.nextDisabled || showCreateAssignee || createAssigneeLoading}>
            {this.isLastSection ? "Submit" : this.quizSubmitting ? "Please wait..." : "Next"}
          </AntButton>
        </>
      );
    }
    let actionText = "Create";
    if (this.hasQuestionnaireTemplates && this.creatingTicket) {
      actionText = "Please wait...";
    }

    return (
      <AntButton type="primary" onClick={this.createWorkItem} disabled={this.createDisabled}>
        {actionText}
      </AntButton>
    );
  }

  get footer() {
    const { currentSectionIndex, sections } = this.state;
    return (
      <AntRow type="flex" justify="space-between">
        {this.showQuestionnaire && <div>{`${currentSectionIndex + 1}/${sections.length} sections`}</div>}
        <div className="issue-actions-container">
          {(this.isLastSection || !this.showQuestionnaire) && (
            <AntButton type="ghost" onClick={this.handleCancel}>
              Cancel
            </AntButton>
          )}
          {this.actionBtn}
        </div>
      </AntRow>
    );
  }

  get template_dropdown() {
    if (this.showQuestionnaire) {
      return null;
    }
    return (
      <AntCol span={8}>
        <SelectRestapi
          style={{ width: "100%" }}
          uri="ticket_templates"
          fetchData={this.props.smartTicketTemplatesList}
          rest_api={this.props.rest_api}
          searchField="name"
          onChange={this.handleTemplateChange}
          value={this.state.template_id || ""}
          placeholder="Template"
          filterOptionMethod={option => {
            return option.enabled;
          }}
        />
      </AntCol>
    );
  }

  get product_dropdown() {
    if (this.showQuestionnaire) {
      return null;
    }
    const { product } = this.state;
    return (
      <AntCol span={8}>
        <SelectRestapi
          searchField="name"
          style={{ width: "100%" }}
          mode={"single"}
          placeholder="Select Project"
          rest_api={this.props.rest_api}
          uri="workspace"
          uuid={"0"}
          value={product || {}}
          onChange={this.handleProductChange}
          labelinValue={true}
          allowClear={false}
        />
      </AntCol>
    );
  }

  get title() {
    let modalTitle = "Create Issue";

    if (this.state.workitem_created && this.activeQuiz) {
      modalTitle = this.activeQuiz.qtemplate_name;
    }

    return (
      <AntRow style={{ marginTop: "10px" }}>
        <AntCol span={16}>
          <Title level={4}>{modalTitle}</Title>
        </AntCol>
        {/*{this.template_dropdown}*/}
        {this.product_dropdown}
      </AntRow>
    );
  }

  get notify() {
    return (
      <AntCheckbox checked={this.state.notify} onChange={e => this.setState({ notify: e.target.checked })}>
        Notify Reporter and Assignee(s)
      </AntCheckbox>
    );
  }

  get body() {
    if (!this.showQuestionnaire) {
      return (
        <AntRow gutter={[16, 16]}>
          <AntCol span={24}>{this.defaultFields}</AntCol>
          <AntCol span={24}>{this.genericFields}</AntCol>
          <AntCol span={24}>{this.notify}</AntCol>
        </AntRow>
      );
    }
    if (!this.activeSection) {
      return null;
    }
    return (
      <AntRow gutter={[16, 16]}>
        <AntCol span={24}>
          <strong>{this.activeSection.name}</strong>
        </AntCol>
        <AntCol span={24}>
          <AntText type="secondary">{this.activeSection.description}</AntText>
        </AntCol>
        <AntCol span={24}>
          <Questions
            showAnsweredBy={false}
            sectionId={this.activeSection.id}
            quiz={this.activeQuiz}
            updateQuiz={this.handleQuizChange}
            downloadFile={this.downloadFile}
            getFileUploading={this.getFileLoading}
            filesDelete={this.props.filesDelete}
          />
        </AntCol>
      </AntRow>
    );
  }

  render() {
    return (
      <AntModal
        visible={this.props.isVisible}
        title={this.title}
        footer={this.footer}
        width={700}
        closable={true}
        maskClosable={false}
        mask={true}
        onCancel={this.handleCancel}>
        <div className={this.props.className} ref={this.modalScrollRef}>
          {this.body}
        </div>
      </AntModal>
    );
  }
}

CreateIssueContainer.propTypes = {
  className: PropTypes.string,
  product: PropTypes.string,
  isVisible: PropTypes.bool,
  onCancelEvent: PropTypes.func.isRequired,
  onSuccessEvent: PropTypes.func.isRequired,
  workitem: PropTypes.object.isRequired,
  parent_id: PropTypes.string,
  allowCreateAssignee: PropTypes.bool,
  location: PropTypes.object
};

CreateIssueContainer.defaultProps = {
  className: "create-issue-modal",
  isVisible: false,
  location: undefined
};

export const mapStateToProps = state => ({
  ...mapRestapiStatetoProps(state),
  workitem: getSmartTicketFieldsSelector(state),
  products: getProductsBulkListSelector(state),
  ...mapUserFormStateToProps(state)
});

export const mapDispatchToProps = dispatch => ({
  ...mapWorkitemsToProps(dispatch),
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapSectionsToProps(dispatch),
  ...mapQuizToProps(dispatch),
  ...mapGenericToProps(dispatch),
  ...mapFormDispatchToPros(dispatch),
  ...mapWorkspaceToProps(dispatch)
});

export default connect(mapStateToProps, mapDispatchToProps)(CreateIssueContainer);
