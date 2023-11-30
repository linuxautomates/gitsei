import { FORM_CLEAR, FORM_INITIALIZE, FORM_UPDATE_FIELD, FORM_UPDATE_OBJ } from "../actions/formActionTypes";

// const INITIAL_STATE = fromJS({
//   user_form: {},
//   kb_form: {},
//   template_form: {},
//   assessment_template_form: {},
//   section_form: undefined,
//   policy_form: undefined,
//   product_form: undefined,
//   jira_mapping_form: undefined,
//   github_mapping_form: undefined,
//   stages_form: [],
//   apikey_form: undefined,
//   stt_form: undefined,
//   propel_form: undefined,
//   workitem_form: undefined
// });

const INITIAL_STATE = {
  user_form: {},
  kb_form: {},
  template_form: {},
  assessment_template_form: {},
  section_form: undefined,
  policy_form: undefined,
  product_form: undefined,
  jira_mapping_form: undefined,
  github_mapping_form: undefined,
  stages_form: [],
  apikey_form: undefined,
  stt_form: undefined,
  propel_form: undefined,
  workitem_form: undefined,
  states_form: undefined,
  automation_rule_form: undefined
};

export const formReducer = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case FORM_CLEAR:
      // console.log(`clearing form ${action.name}`);
      delete state[action.name];
      //return state.delete(action.name);
      return { ...state };
    case FORM_INITIALIZE:
      state[action.name] = action.data || INITIAL_STATE[action.name] || {};
      //return state.set(action.name, INITIAL_STATE.toJS()[action.name]);
      return state;
    case FORM_UPDATE_FIELD:
      return {
        ...state,
        [action.name]: {
          ...(state[action.name] || {}),
          [action.field]: action.value
        }
      };
    //set(state, `${action.name}.${action.field}`, action.value);
    //return state.setIn([action.name, action.field], action.value);
    case FORM_UPDATE_OBJ:
      return {
        ...state,
        [action.name]: Object.assign(Object.create(Object.getPrototypeOf(action.obj)), action.obj)
      };
    //state[action.name] = cloneDeep(action.obj);
    //state[action.name] = Object.assign(Object.create(Object.getPrototypeOf(action.obj)), action.obj);
    //return state;
    //return cloneDeep(state);
    //return state.set(action.name, action.obj);
    default:
      return state;
  }
};
