import { all, put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import * as formActions from "reduxConfigs/actions/formActions";
import * as paginationActions from "reduxConfigs/actions/paginationActions";
import { PROPEL_FETCH, PROPEL_NEW } from "../actions/propelLoad.actions";
import { getData, getError } from "utils/loadingUtils";
import { RestPropel } from "classes/RestPropel";
import { getContentType, PRIMITIVE_CONTENT_TYPES, RestPropelField } from "../../classes/RestPropel";
import { get } from "lodash";
import { notification } from "antd";
import {
  DYNAMIC_MULTI_CUSTOM_SELECT_TYPE,
  DYNAMIC_MULTI_SELECT_TYPE,
  DYNAMIC_SINGLE_CUSTOM_SELECT_TYPE,
  DYNAMIC_SINGLE_SELECT_TYPE,
  CONIFG_TABLE_FILTER_TYPE,
  CONFIG_TABLE_COLUMN_TYPE
} from "../../constants/fieldTypes";
import { getModifiedTrigger } from "./propels/helper";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

const dynamicTypes = [
  DYNAMIC_MULTI_CUSTOM_SELECT_TYPE,
  DYNAMIC_SINGLE_CUSTOM_SELECT_TYPE,
  DYNAMIC_SINGLE_SELECT_TYPE,
  DYNAMIC_MULTI_SELECT_TYPE
];

const restapiState = state => state.restapiReducer;

export function* propelFetchEffectSaga(action) {
  // console.log(action);
  const propelId = action.id;
  const complete = `PROPEL_${propelId}`;
  yield put(actionTypes.genericGet("propels", propelId, complete));
  yield take(complete);
  const apiState = yield select(restapiState);
  if (getError(apiState, "propels", "get", propelId)) {
    handleError({
      showNotfication: true,
      message: "Failed to fetch propel",
      bugsnag: {
        severity: severityTypes.WARNING,
        context: issueContextTypes.PROPELS,
        data: { action, propelId, apiState }
      }
    });
    return;
  }
  const propel = getData(apiState, "propels", "get", propelId);
  let chart = new RestPropel(propel);
  let idCalls = {};

  if (chart.trigger_template_type) {
    yield put(
      actionTypes.genericGet(
        "propel_trigger_templates",
        chart.trigger_template_type,
        `COMPLETE_propel_trigger_templates`,
        false
      )
    );
    yield take(`COMPLETE_propel_trigger_templates`);
  }

  let nodeTemplates = [];
  Object.keys(chart.nodes).forEach(node => {
    if (node !== "0" && !nodeTemplates.includes(chart.nodes[node].type)) {
      nodeTemplates.push(chart.nodes[node].type);
    }
  });

  // console.log(nodeTemplates);
  yield put(
    actionTypes.genericList(
      "propel_node_templates",
      "list",
      {
        filter: { types: nodeTemplates }
      },
      `COMPLETE_propel_node_templates`,
      false
    )
  );
  yield take(`COMPLETE_propel_node_templates`);

  const templateapiState = yield select(restapiState);

  if (getError(templateapiState, "propel_trigger_templates", "get", chart.trigger_template_type)) {
    handleError({
      showNotfication: true,
      message: "Failed to load propel trigger template",
      bugsnag: {
        severity: severityTypes.WARNING,
        context: issueContextTypes.PROPELS,
        data: { action, templateapiState }
      }
    });
    return;
  }
  if (getError(templateapiState, "propel_node_templates", "list", "0")) {
    handleError({
      showNotfication: true,
      message: "Failed to load propel templates",
      bugsnag: {
        severity: severityTypes.WARNING,
        context: issueContextTypes.PROPELS,
        data: { action, templateapiState }
      }
    });
    return;
  }
  const templates = getData(templateapiState, "propel_node_templates", "list", "0").records || [];
  let triggerTemplate = getData(templateapiState, "propel_trigger_templates", "get", chart.trigger_template_type);
  triggerTemplate = getModifiedTrigger(triggerTemplate ?? {});
  let contentTypes = [];

  // TODO: Update use_input_fields values here
  //chart.updateInputFields();

  Object.keys(chart.nodes).forEach(node => {
    // make rest calls for your input here
    // for each node in the chart, find the node_template associated with it
    const template = templates.find(template => chart.nodes[node].type === template.type) || { input: {} };
    chart.nodes[node].properties = {
      ...template.ui_data
    };

    if (node === "0") {
      chart.nodes[node].properties = {
        ...triggerTemplate.ui_data
      };
      chart.nodes[node].name = triggerTemplate.display_name;
      chart.nodes[node].description = triggerTemplate.description;
      chart.nodes[node].output = Object.keys(triggerTemplate.fields).reduce((map, obj) => {
        map[obj] = triggerTemplate.fields[obj];
        return map;
      }, {});
      // TODO: get the content schema for trigger output fields
      Object.keys(chart.nodes[node].output).forEach(field => {
        const fieldObj = chart.nodes[node].output[field];
        const type = getContentType(fieldObj.content_type);
        if (
          fieldObj.content_type &&
          !PRIMITIVE_CONTENT_TYPES.includes(fieldObj.content_type) &&
          !contentTypes.includes(type)
        ) {
          contentTypes.push(type);
        }
      });
      Object.keys(triggerTemplate.fields || {}).forEach(field => {
        const nodeField = chart.nodes[node].input[field];
        let nodeJson = { values: [] };
        if (nodeField) {
          nodeJson = chart.nodes[node].input[field].json;
        }

        const fieldObj = new RestPropelField({
          ...nodeJson,
          ...get(triggerTemplate, ["fields", field], {})
        });
        if (fieldObj.dynamic_resource_name !== undefined && dynamicTypes.includes(fieldObj.type)) {
          // console.log(fieldObj.json);
          const uri = fieldObj.dynamic_resource_name;
          let difference = [];
          if (idCalls[uri] === undefined) {
            idCalls[uri] = [];
            //difference = fieldObj.values.map(value => value.key);
            difference = fieldObj.values.filter(
              x => !(x.key || "").includes("custom|") && !(x.key || "").includes("${")
            );
          } else {
            difference = fieldObj.values.filter(
              x => !(x.key || "").includes("custom|") && !(x.key || "").includes("${") && !idCalls[uri].includes(x.key)
            );
            // do a generic get here
          }
          if (difference.length > 0) {
            idCalls[uri].push(...difference);
          }
        } else if (fieldObj.type === "config-table-filter") {
          const uri = "config_tables";
          const tableId = fieldObj.values[0].table_id;
          if (tableId) {
            if (idCalls[uri] === undefined) {
              idCalls[uri] = [tableId];
            } else {
              if (!idCalls[uri].includes(tableId)) {
                idCalls[uri].push(tableId);
              }
            }
          }
        }
        chart.nodes[node].input[field] = fieldObj;
        if (!get(triggerTemplate, ["fields", field], undefined)) {
          delete chart.nodes[node].input[field];
        }
      });
    } else {
      chart.nodes[node].description = template.description;
      //chart.nodes[node].name = template.name;
      chart.nodes[node].output = JSON.parse(JSON.stringify(template.output));
      // TODO: get the content type of each output variable and fetch them
      Object.keys(template.output).forEach(field => {
        const fieldObj = template.output[field];
        const type = getContentType(fieldObj.content_type);
        if (
          fieldObj.content_type &&
          !PRIMITIVE_CONTENT_TYPES.includes(fieldObj.content_type) &&
          !contentTypes.includes(type)
        ) {
          contentTypes.push(type);
        }
      });
      chart.nodes[node].options = template.options || [];
    }

    // for each input field in the node template, populate chart input fields
    Object.keys(template.input).forEach(field => {
      const templateField = get(template, ["input", field], {});
      const nodeField = chart.nodes[node].input[field];
      const type = getContentType(templateField.content_type);
      let nodeJson = { values: [] };
      // TODO: get content types for all the input fields also
      if (
        templateField.content_type &&
        !PRIMITIVE_CONTENT_TYPES.includes(templateField.content_type) &&
        !contentTypes.includes(type)
      ) {
        contentTypes.push(type);
      }
      if (nodeField) {
        nodeJson = chart.nodes[node].input[field].json;
      }

      const fieldObj =
        node !== "0"
          ? new RestPropelField({
              ...nodeJson,
              ...templateField
            })
          : new RestPropelField({
              ...nodeJson,
              ...get(triggerTemplate, ["fields", field], {})
            });
      if (fieldObj.dynamic_resource_name !== undefined && dynamicTypes.includes(fieldObj.type)) {
        // console.log(fieldObj.json);
        const uri = fieldObj.dynamic_resource_name;
        let difference = [];
        if (idCalls[uri] === undefined) {
          idCalls[uri] = [];
          //difference = fieldObj.values.map(value => value.key);
          difference = fieldObj.values.filter(x => !(x.key || "").includes("custom|") && !(x.key || "").includes("${"));
        } else {
          difference = fieldObj.values.filter(
            x => !(x.key || "").includes("custom|") && !(x.key || "").includes("${") && !idCalls[uri].includes(x.key)
          );
          // do a generic get here
        }
        if (difference.length > 0) {
          idCalls[uri].push(...difference);
        }
      } else if (fieldObj.type === "config-table-filter") {
        const uri = "config_tables";
        const tableId = get(fieldObj.values[0], ["table_id"]);
        //const tableId = fieldObj.values[0].table_id;
        if (tableId) {
          if (idCalls[uri] === undefined) {
            idCalls[uri] = [`${tableId}?expand=schema`];
          } else {
            if (!idCalls[uri].includes(`${tableId}?expand=schema`)) {
              idCalls[uri].push(`${tableId}?expand=schema`);
            }
          }
        }
      }
      chart.nodes[node].input[field] = fieldObj;
    });
  });

  // TODO: make the content-types list call here and add the content type schema to each of the nodes
  if (contentTypes.length > 0) {
    const contentComplete = `COMPLETE_CONTENT_${chart.id}`;
    yield put(
      actionTypes.genericList(
        "content_schema",
        "list",
        { content_types: contentTypes },
        contentComplete,
        chart.id,
        false
      )
    );
    yield take(contentComplete);
  }

  const numCalls = Object.keys(idCalls).reduce((num, calls) => {
    num = num + idCalls[calls].length;
    return num;
  }, 0);

  if (numCalls > 0) {
    yield all(
      Object.keys(idCalls).reduce((actions, uri) => {
        actions.push(
          ...idCalls[uri]
            .filter(id => id && !id.toString().includes("custom|") && !id.toString().includes("${"))
            .map(id => put(actionTypes.genericGet(uri, id, `COMPLETE_${uri}_${id}`)))
        );
        return actions;
      }, [])
    );

    // now wait for every one of those things to complete
    const allActions = Object.keys(idCalls).reduce((actions, uri) => {
      actions.push(
        ...idCalls[uri]
          .filter(id => id && !id.toString().includes("custom|") && !id.toString().includes("${"))
          .map(id => `COMPLETE_${uri}_${id}`)
      );
      return actions;
    }, []);
    // const allActions = Object.keys(idCalls).map(uri => {
    //   return idCalls[uri].map(id => `COMPLETE_${uri}_${id}`);
    // });
    // console.log(allActions);
    yield all(allActions.map(action => take(action)));
  }

  // now that all the actions are complete, now try to fill them back in as values

  const newapiState = yield select(restapiState);

  // TODO for fields of type config-table-filter, just add the schema into the values

  const contentSchemas = getData(newapiState, "content_schema", "list", chart.id).records || [];
  // console.log(chart.orderedNodes);
  chart.orderedNodes.forEach(node => {
    Object.keys(chart.nodes[node].input).forEach(field => {
      // TODO: Add content schema for each input field
      const fieldObj = chart.nodes[node].input[field];
      const type = getContentType(fieldObj.content_type);
      const contentSchema = contentSchemas.find(schema => schema.content_type === type);
      fieldObj.content_schema = contentSchema;
      if (fieldObj.dynamic_resource_name && dynamicTypes.includes(fieldObj.type)) {
        const newValues = fieldObj.values.map(value => {
          if (value && value.toString().includes("custom|")) {
            return { label: value.replace("custom|", ""), key: value };
          }
          if (value && value.toString().includes("${")) {
            return { label: value, key: value };
          }
          if (!getError(newapiState, fieldObj.dynamic_resource_name, "get", value)) {
            const data = getData(newapiState, fieldObj.dynamic_resource_name, "get", value);
            const searchField = fieldObj.search_field ? fieldObj.search_field : "name";
            return { label: data[searchField], key: value };
          } else {
            handleError({
              showNotfication: true,
              message: `Invalid configuration for ${fieldObj.display_name}`,
              bugsnag: {
                message: "invalid configuation",
                severity: severityTypes.WARNING,
                context: issueContextTypes.PROPELS,
                data: { action, templateapiState }
              }
            });
            return undefined;
          }
        });
        fieldObj.values = newValues.filter(value => value !== undefined);
      } else if (fieldObj.type === CONIFG_TABLE_FILTER_TYPE || fieldObj.type === CONFIG_TABLE_COLUMN_TYPE) {
        const uri = "config_tables";
        //const tableId = fieldObj.values[0].table_id;
        // now get the table id from the input
        if (fieldObj.use_input_fields && fieldObj.use_input_fields.table_id) {
          const tableIdObj = chart.nodes[node].input[fieldObj.use_input_fields.table_id];
          const tableId = get(tableIdObj.values, [0]);
          if (typeof tableId == "string") {
            if (!getError(newapiState, uri, "get", `${tableId}?expand=schema`)) {
              const data = getData(newapiState, uri, "get", `${tableId}?expand=schema`);
              const columns = get(data, ["schema", "columns"], {});
              let fields = Object.keys(columns).reduce((acc, obj) => {
                acc[obj] = {
                  key: columns[obj].key,
                  content_type: "string",
                  value_type: "string"
                };
                return acc;
              }, {});
              fields.id = {
                key: "id",
                content_type: "id:config_row",
                value_type: "string"
              };
              const schema = {
                content_type: `config-tables/${tableId}`,
                key: tableId,
                fields: fields,
                value_type: "json_blob"
              };
              if (fieldObj.values?.length > 0) {
                fieldObj.values[0].schema = schema;
              }
            }
          }
        }
      }
    });

    // TODO: Add content schema for each output field
    Object.keys(chart.nodes[node].output).forEach(field => {
      const fieldObj = chart.nodes[node].output[field];
      const type = getContentType(fieldObj.content_type);
      const contentSchema = contentSchemas.find(schema => schema.content_type === type);
      // console.log(field);
      // console.log(contentSchema);
      chart.nodes[node].output[field].content_schema = contentSchema;
    });
    // TODO: if output field is dependent on content type of another field, then do that also
    chart.updateOutputContentTypes(node);
  });

  // TODO: Update use_input_fields values here
  //chart.updateInputFields();

  // Object.keys(chart.nodes).forEach(node => {
  //   chart.updateOutputContentTypes(node);
  // });

  yield put(actionTypes.restapiClear("content_schema", "list", chart.id));

  // console.log(JSON.stringify(chart.post_data, null, 4));

  //yield put(actionTypes.genericList("propel_node_templates", "list", {}));

  // now take this chart object and put it in a form reducer
  yield put(formActions.formUpdateObj("propel_form", chart));

  //yield put(actionTypes.genericList("propel_trigger_templates", "list", {}, null, "0"));
  yield put(paginationActions.paginationGet("propel_trigger_templates", "list", {}, "0"));
  yield put(actionTypes.propelNodeCategoriesGet("list", `COMPLETE_NODE_CATEGORIES`));
  yield put(paginationActions.paginationGet("propel_node_templates", "list", { filter: { hidden: false } }, "0"));
  //yield put(actionTypes.genericList("propel_node_templates", "list", { page_size: 1000 }, null, "0"));

  // console.log("exiting the fetch saga");
}

export function* propelNewEffectSaga(action) {
  // console.log(action);
  const triggerType = action.trigger_type;
  const complete = `PROPEL_TEMPLATES_GET_${triggerType}`;
  yield put(actionTypes.genericGet("propel_trigger_templates", triggerType, complete));
  yield take(complete);
  const apiState = yield select(restapiState);
  if (getError(apiState, "propel_trigger_templates", "get", triggerType)) {
    // do some error handling here
    handleError({
      showNotfication: true,
      message: `Failed to load propel trigger template ${triggerType}`,
      bugsnag: {
        message: "Failed to load propel trigger template",
        severity: severityTypes.WARNING,
        context: issueContextTypes.PROPELS,
        data: { action }
      }
    });
    return;
  }

  let triggerRecord = getData(apiState, "propel_trigger_templates", "get", triggerType);
  triggerRecord = getModifiedTrigger(triggerRecord);
  let chart = new RestPropel();

  chart.nodes["0"].name = triggerRecord.display_name;
  chart.nodes["0"].description = triggerRecord.description;
  chart.nodes["0"].trigger_type = triggerRecord.type || triggerRecord.trigger_type;
  chart.nodes["0"].trigger_template_type = triggerRecord.type;
  chart.nodes["0"].properties = {
    ...triggerRecord.ui_data
  };
  chart.nodes["0"].input = Object.keys(triggerRecord.fields).reduce((map, obj) => {
    // console.log(triggerRecord.fields[obj]);
    map[obj] = new RestPropelField({
      ...triggerRecord.fields[obj],
      values: []
    });
    return map;
  }, {});
  chart.nodes["0"].output = Object.keys(triggerRecord.fields).reduce((map, obj) => {
    map[obj] = triggerRecord.fields[obj];
    return map;
  }, {});

  chart.trigger_type = triggerRecord.trigger_type;
  chart.trigger_template_type = triggerRecord.type;
  //chart.nodes["0"] = triggerNode;

  //TODO: for each output node get the content schema and update it
  let contentTypes = [];
  Object.keys(chart.nodes["0"].output).forEach(field => {
    const fieldObj = chart.nodes["0"].output[field];
    const type = getContentType(fieldObj.content_type);
    if (
      fieldObj.content_type !== undefined &&
      !PRIMITIVE_CONTENT_TYPES.includes(fieldObj.content_type) &&
      !contentTypes.includes(type)
    ) {
      contentTypes.push(type);
    }
  });

  if (contentTypes.length > 0) {
    const contentComplete = "PROPEL_TRIGGER_CONTENT_TYPES";
    yield put(
      actionTypes.genericList(
        "content_schema",
        "list",
        { filter: { content_types: contentTypes } },
        contentComplete,
        "new_trigger"
      )
    );
    yield take(contentComplete);
    const contentState = yield select(restapiState);
    const contentSchemas = getData(contentState, "content_schema", "list", "new_trigger").records || [];
    Object.keys(chart.nodes["0"].output).forEach(field => {
      const fieldObj = chart.nodes["0"].output[field];
      let type = getContentType(fieldObj.content_type);
      const contentSchema = contentSchemas.find(schema => type === schema.content_type);
      if (contentSchema) {
        fieldObj.content_schema = contentSchema;
      }
    });

    yield put(actionTypes.restapiClear("content_schema", "list", "new_trigger"));
  }

  // console.log(JSON.stringify(chart.post_data, null, 4));
  //
  // console.log("Updating chart");

  yield put(paginationActions.paginationGet("propel_trigger_templates", "list", {}, "0"));
  yield put(actionTypes.propelNodeCategoriesGet("list", `COMPLETE_NODE_CATEGORIES`));
  yield put(paginationActions.paginationGet("propel_node_templates", "list", { filter: { hidden: false } }, "0"));

  yield put(formActions.formUpdateObj("propel_form", chart));
}

export function* propelFetchWatcherSaga() {
  yield takeLatest([PROPEL_FETCH], propelFetchEffectSaga);
}

export function* propelNewWatcherSaga() {
  yield takeLatest([PROPEL_NEW], propelNewEffectSaga);
}
