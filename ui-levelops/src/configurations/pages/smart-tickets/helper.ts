// @ts-ignore
import sanitizeHtml from "sanitize-html";

export const sanitizeOptions = {
  allowedTags: [],
  allowedAttributes: {},
  allowedSchemes: [],
  allowProtocolRelative: false
};

export const sanitizeHtmlAndLink = (text: string) => {
  return sanitizeHtml(text, sanitizeOptions).replace(/(?:https?|ftp):\/\/[\n\S]+/g, "");
};
