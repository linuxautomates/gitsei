import React from "react";
import mustache from "mustache";
import { get } from "lodash";
import { StringKeys, useStringsContext } from "@harness/microfrontends/dist/framework/strings";
import envConfig from "env-config";

export interface UseStringsReturn {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getString(key: StringKeys, vars?: Record<string, any>): string;
}

export function useStrings(): UseStringsReturn {
  const { data: strings, getString } = useStringsContext();

  return {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    getString(key: StringKeys, vars: Record<string, any> = {}) {
      if (typeof getString === "function") {
        return getString(key, vars);
      }

      const template = get(strings, key);

      if (typeof template !== "string") {
        throw new Error(`No valid template with id "${key}" found in any namespace`);
      }

      return mustache.render(template, { ...vars, $: strings });
    }
  };
}

type NewType = JSX.IntrinsicElements;

export interface StringProps extends React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement> {
  stringID: StringKeys;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  vars?: Record<string, any>;
  useRichText?: boolean;
  tagName: keyof NewType;
}

export function String(props: StringProps): React.ReactElement | null {
  const { stringID, vars, useRichText, tagName: Tag, ...rest } = props;
  const { getString } = useStrings();

  try {
    const text = getString(stringID, vars);

    return useRichText ? (
      <Tag {...(rest as unknown as {})} dangerouslySetInnerHTML={{ __html: text }} />
    ) : (
      <Tag {...(rest as unknown as {})}>{text}</Tag>
    );
  } catch (e) {
    if (envConfig.get("NODE_ENV") !== "production") {
      return <Tag style={{ color: "var(--red-500)" }}>{get(e, "message", e)}</Tag>;
    }

    return null;
  }
}

String.defaultProps = {
  tagName: "span"
};
