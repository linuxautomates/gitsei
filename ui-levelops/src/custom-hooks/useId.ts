import { useState } from "react";
import getUniqueId from "utils/uniqueID";

const useId = () => {
  const [id] = useState<string>(getUniqueId());
  return id;
};

export default useId;
