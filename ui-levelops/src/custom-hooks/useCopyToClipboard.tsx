import { useToaster } from "@harness/uicore";

export function useCopyToClipboard(): { copyToClipboard: (text: string) => void } {
  const { showSuccess, showError } = useToaster();

  const copyToClipboard = async (source: string): Promise<void> => {
    try {
      await navigator.clipboard.writeText(source);
      showSuccess("Successfully copied to clipboard");
    } catch (ex) {
      showError("Copy to clipboard has failed");
    }
  };

  return { copyToClipboard };
}
