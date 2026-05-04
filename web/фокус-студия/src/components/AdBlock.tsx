import React, { useEffect } from 'react';

export const AdBlock: React.FC = () => {
  useEffect(() => {
    if ((window as any).yaContextCb) {
      (window as any).yaContextCb.push(() => {
        if ((window as any).Ya?.Context?.AdvManager) {
          (window as any).Ya.Context.AdvManager.render({
            "blockId": "R-A-19198612-1",
            "renderTo": "yandex_rtb_R-A-19198612-1"
          });
        }
      });
    }
  }, []);

  return (
    <div className="w-full flex justify-center py-6 min-h-[100px]">
      <div id="yandex_rtb_R-A-19198612-1"></div>
    </div>
  );
};
