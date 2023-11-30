import React from "react";
import { isJavaScriptProtocol } from "../../constants/xss";
// @ts-ignore
import sanitizeHtml from "sanitize-html";
import { unescape } from "lodash";
import { validateURL } from "utils/stringUtils";

interface SanitizedLinkProps {
  url: string;
}

const SanitizedLink: React.FC<SanitizedLinkProps> = ({ url }) => {
  const decodedURL = unescape(decodeURIComponent(url));
  return (
    <>
      {url && sanitizeHtml(url) && !isJavaScriptProtocol.test(url) && validateURL(url) ? (
        <a href={decodedURL} target="_blank" rel="noopener noreferrer">
          {decodedURL}
        </a>
      ) : (
        url
      )}
    </>
  );
};

export default SanitizedLink;
