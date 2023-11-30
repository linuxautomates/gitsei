/*eslint-disable-next-line*/
export const REGEX_COMMENTS_MENTION = /(\@\[(?:(?:(?:[^<>()\[\]\.,;:\s@\"]+(?:\.[^<>()\[\]\.,;:\s@\"]+)*)|(?:\".+\"))@(?:(?:[^<>()[\]\.,;:\s@\"]+\.)+[^<>()[\]\.,;:\s@\"]{2,}))\])/gi;
/*eslint-disable-next-line*/
export const REGEX_COMMENTS_EMAIL = /\@\[((?:(?:[^<>()\[\]\.,;:\s@\"]+(?:\.[^<>()\[\]\.,;:\s@\"]+)*)|(?:\".+\"))@(?:(?:[^<>()[\]\.,;:\s@\"]+\.)+[^<>()[\]\.,;:\s@\"]{2,}))\]/gi;
/*eslint-disable-next-line*/
export const REGEX_PHONE_NUMBER = /^[^a-zA-Z]+$/g;

export const checkMFAValidation = code => /^[0-9]{6,6}$/.test(code);

export const removeLastSlash = url => url ? url.replace(/\/$/, "") : "";
