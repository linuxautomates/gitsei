import { RestWorkItem } from "classes/RestWorkItem";
import { DYNAMIC_MULTI_SELECT_TYPE, DYNAMIC_SINGLE_SELECT_TYPE } from "constants/fieldTypes";
import { filter, get, map } from "lodash";

export const updatedWorkItem = (data: any) => {
  const { attachments, assignees, templateFields, values, tags, ticketType, workItem } = data;
  const work_item = new RestWorkItem({ ...workItem.json?.() });
  const ticketFields = map(templateFields, (field: any) => {
    const fieldValue = get(values, [field.ticket_field_id, "value"], []);
    const val = map(
      filter(fieldValue, (v: any) => v !== undefined || v?.key !== undefined),
      (v: any) => {
        let value = v;
        if ([DYNAMIC_SINGLE_SELECT_TYPE, DYNAMIC_MULTI_SELECT_TYPE].includes(field.type)) {
          value = v.key;
        }
        return {
          value,
          type: field.type === "date" ? "int" : "string"
        };
      }
    );
    return { id: field.id, ticket_field_id: field.ticket_field_id, values: val };
  });
  work_item.assignees = map(assignees, (assignee: any) => ({ user_id: assignee.key, user_email: assignee.label }));
  work_item.attachments = map(attachments, (attachment: any) => ({
    upload_id: attachment.response ? attachment.response.id : attachment.upload_id,
    file_name: attachment.response ? attachment.response.file_name : attachment.file_name
  }));
  work_item.ticket_data_values = ticketFields;
  work_item.tag_ids = tags ? map(tags, (t: any) => t.key) : null;
  work_item.ticket_type = ticketType;
  return work_item;
};

export enum WORKITEM_ACTION {
  WORKITEM_DELETE = "workItemDelete",
  WORKITEM_MENU_CLICK = "workItemMenuClick",
  WORKITEM_NAME_UPDATE = "workItemNameUpdate",
  WORKITEM_DESCRIPTION_UPDATE = "workItemDescriptionUpdate",
  WORKITEM_UPDATE = "workItemUpdate",
  WORKITEM_FILE_CHANGE = "workItemFileChange",
  WORKITEM_FILE_ADD = "workItemFileAdd",
  WORKITEM_FILE_REMOVE = "workItemFileRemove",
  WORKITEM_FILE_DOWNLOAD = "workItemFileDownload",
  WORKITEM_FILE_PREVIEW = "workItemFilePreview",
  WORKITEM_FILE_CLOSE_PREVIEW = "workItemFileClosePreview",
  TICKET_TYPE_CHANGE = "ticketTypeChange",
  ASSIGNEE_CHANGE = "assigneeChange",
  WORKITME_STATUS_CHANGE = "workItemStatusChange",
  WORKITEM_DUE_DATE_CHANGE = "workItemDueDateChange",
  UPDATED_TAGS = "updatedTags",
  WORKITEM_PRODUCT_CHANGE = "workItemProductChange",
  TOGGLE_MORE_CUSTOM_FIELDS = "toggleMoreCustomFields",
  UPDATE_CUSTOM_FIELDS = "updateCustomFields"
}
