import { capitalize, lowerCase, upperCase } from "lodash";

export function valuetoLabel(value) {
  return (value || "").toUpperCase().replace("_", " ");
}

export function valueToTitle(value) {
  return (value || "").replace(/_/g, " ");
}

export function validateEmail(value) {
  if (!value) {
    return false;
  }
  return value.match(/^([\w.%+-]+)@([\w-]+\.)+([\w]{2,})$/i);
}

export function validatePassword(value) {
  if (!value) {
    return false;
  }
  let validPassword = value.length > 8;
  validPassword = validPassword && value.match(/[A-Z]+/i);
  validPassword = validPassword && value.match(/[0-9]/i);
  return validPassword;
}

// https://regex101.com/r/kIowvx/23
export const urlPattern = new RegExp(
  "(" +
    "(" +
    "^(https?:\\/\\/)?" + // protocol
    "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|" + // domain name
    "((\\d{1,3}\\.){3}\\d{1,3}))" + // OR ip (v4) address
    "(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*" + // port and path
    "(\\?[;&a-z\\d%_.~+=-]*)?" + // query string
    "(\\#[-a-z\\d_]*)?$" +
    ")" +
    "|" +
    "(" +
    "(^(https?:\\/\\/)?)" + // protocol
    "([\\w-]+)" + // domain name
    "(:[\\d]+)" + // port
    "(\\/[-a-zd%_.~+/]*" + // optional path
    "(\\?[;&a-z\\d%_.~+=-]*)?)?$" + // query string excluding leading forward slash
    ")" +
    ")",
  "i"
); // fragment locator

export function validateURL(str) {
  return !!urlPattern.test(str);
}

/**
 * It returns true if the string passed to it is a valid markdown link
 * @param str - The string to test.
 * @returns A boolean value.
 */
export function validMarkdownLink(str) {
  const markdownLinkRegex = new RegExp(/!?\[([^\]]*)\]\(([^\)]+)\)/gm);
  return !!markdownLinkRegex.test(str);
}

// a set of exactly 4 octets
// an octet is a number between 0 to 255, followed by either a dot or the end of string
// zero is not allowed as the first octet or as padding for any other
export const ipv4Pattern = new RegExp(/^(?!0)(?!.*\.$)((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]?|0)(\.|$)){4}$/);

export function validateIP(str) {
  return !!ipv4Pattern.test(str);
}

export const portPattern = new RegExp(
  /^([1-9]|[1-5]?[0-9]{2,4}|6[1-4][0-9]{3}|65[1-4][0-9]{2}|655[1-2][0-9]|6553[1-5])$/
);

export function validatePort(str) {
  return !!portPattern.test(str);
}

export function removeNewLine(str) {
  return str.replace("\n", "");
}

export function toTitleCase(input) {
  return input
    ?.toString()
    ?.toLowerCase()
    ?.split(/[_ ]/)
    ?.map(part => part?.charAt(0)?.toUpperCase() + part?.slice(1))
    ?.join(" ");
}

export function stringTransform(input, separator, glue = "", lowercaseRestString = true) {
  if (!separator) separator = "_";
  return (
    input
      ?.split(separator)
      .map(
        part =>
          (part.charAt(0) || "").toUpperCase() + (lowercaseRestString ? part.slice(1).toLowerCase() : part.slice(1))
      )
      .join(glue) || ""
  );
}

export const insertAt = (str, sub, pos) =>
  str !== undefined && str !== null ? `${str.slice(0, pos)}${sub}${str.slice(pos)}` : "";

export const truncateAndEllipsis = (data, upto = 15, leftEllipsis = false) => {
  if (!data || data.length <= upto) {
    return data;
  }
  const ellipsis = "...";
  if (leftEllipsis) {
    return ellipsis.concat(data.slice(data.length - upto));
  }
  return data.slice(0, upto).concat(ellipsis);
};

export const createMarkup = html => {
  return { __html: html };
};

export const makeCSVSafeString = str => {
  if (typeof str !== "string") {
    return "";
  }
  // https://en.wikipedia.org/wiki/Comma-separated_values#Example
  let result = str.replace(/\"/g, '""');

  result = `"${result}"`;
  return result;
};

export const capitalizeFirstLetter = text => {
  text = text?.toLowerCase();
  return (text.charAt(0) || "").toUpperCase() + text.slice(1);
};

export const convertCase = (value, labelCase) => {
  if (!labelCase) return value;

  switch (labelCase) {
    case "lower_case":
      return lowerCase(value);
    case "title_case":
      return capitalize(value);
    case "upper_case":
      return upperCase(value);
    default:
      return value;
  }
};

export const joinUrl = (value1 = "", value2 = "", defaultValue) => {
  let joinedValue = "";

  if (typeof value1 !== "string" || typeof value2 !== "string") {
    joinedValue = defaultValue !== undefined ? defaultValue : joinedValue;
  } else {
    const val1_lastchar = value1.slice(-1);
    const val2_firstchar = value2.charAt(0);

    if (val1_lastchar === "/" && val2_firstchar === "/") {
      joinedValue = value1 + value2.substring(1);
    } else {
      joinedValue = value1 + value2;
    }
  }

  return joinedValue;
};

export const b64DecodeUnicode = str => {
  return decodeURIComponent(
    Array.prototype.map
      .call(atob(str), function (c) {
        return "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2);
      })
      .join("")
  );
};

export function strReplaceAll(str, find, replace) {
  return str.replace(new RegExp(find.replace(/[-\/\\^$*+?.()|[\]{}]/g, "\\$&"), "g"), replace);
}

export const getNameInitials = str => {
  const stringSplitted = str.split(" ");
  let n = stringSplitted.length;
  if (n) {
    if (n != 1) {
      return stringSplitted[0].charAt(0).toUpperCase() + stringSplitted[n - 1].charAt(0).toUpperCase();
    } else {
      return str.substring(0, 2).toUpperCase();
    }
  }
};

export const isValidRegEx = formattedFilterText => {
  let isValid = true;
  try {
    formattedFilterText = new RegExp(formattedFilterText);
  } catch (e) {
    isValid = false;
  }
  return isValid;
};

export const sanitizeRegexString = value => {
  if (!value) {
    return "";
  }

  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
};

/**
 * capitalize the part of the string.
 * @param {string} input input string
 * @param {string | undefined} word word to capitalise in the string
 * @returns input with capitalized word mentioned.
 */
export function capitalizeWord(input, word) {
  if (word?.length) {
    const wordInCaps = word.toUpperCase();
    return input
      .split(" ")
      .map(currentWord => (currentWord.toUpperCase() === wordInCaps ? wordInCaps : currentWord))
      .join(" ");
  }
  return input;
}

export const strIsEqual = (str1, str2) => {
  return str1?.trim()?.toUpperCase() === str2?.trim()?.toUpperCase();
};

export const stringSort = (a, b) => {
  if (a > b) return 1;
  if (a < b) return -1;
  return 0;
};

export const getSubString = (str, elipsisLength) => {
  return str.substring(0, elipsisLength);
};

export const getElipsis = (str, elipsisLength) => {
  return str.length > elipsisLength ? "..." : "";
};

/** This functions checks the existance of invalid character in the string*/
export const stringContainsInvalidChars = (name, invalidChars = ["/", "\\"]) => {
  if (typeof name !== "string") return false;
  let invalidCharsExists = false;
  for (let i = 0; i < invalidChars.length; i++) {
    const char = invalidChars[i];
    if (name.includes(char)) {
      invalidCharsExists = true;
      break;
    }
  }
  return invalidCharsExists;
};

export const slugifyId = id => {
  return id?.toLowerCase().replace(/ /g, "_");
};

export const slugifyIdWithoutAlteringCase = id => {
  return id?.replace(/ /g, "_");
};

/**
 * taken from here https://stackoverflow.com/questions/6122571/simple-non-secure-hash-function-for-javascript
 *
 * Returns a hash code from a string
 * @param  {String} str The string to hash.
 * @return {Number}    A 32bit integer
 * @see http://werxltd.com/wp/2010/05/13/javascript-implementation-of-javas-string-hashcode-method/
 */
export const hashCode = stringToHash => {
  if (!!stringToHash.length) {
    let hash = 0;
    for (let i = 0, len = stringToHash.length; i < len; i++) {
      let chr = stringToHash.charCodeAt(i);
      hash = (hash << 5) - hash + chr;
      hash |= 0; // Convert to 32bit integer
    }
    return hash;
  }
  return false;
};

/**
 *
 * Returns string with number escaping all other chars
 * @param  {String} str input string
 * @return {string} without number if contains only char then returns input string only
 */
export const numberFromString = str => {
  const numberPattern = /\d+/g;
  const res = str.match(numberPattern)?.join("");
  return res ? res : str;
};

/**
 *
 * Returns string truncated length
 * @param  {string | number} str input string
 * @return {string} return string
 */
export const truncatedString = (str, truncateLen = 10) => {
  return str ? `${str}`?.slice(0, truncateLen) : "";
};
