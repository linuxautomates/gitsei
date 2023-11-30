import { useEffect, useRef } from "react";

const useOutsideClickDetector = <P extends HTMLElement>(handler: () => void) => {
  const ref = useRef<P>(null);
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current!.contains(event.target as any)) {
        handler();
      }
    }

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      // Unbind the event listener on clean up
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  return ref;
};

export default useOutsideClickDetector;
