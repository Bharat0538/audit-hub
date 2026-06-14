import type { FieldChange } from '../../types';

interface DiffViewerProps {
  changes: FieldChange[];
}

export function DiffViewer({ changes }: DiffViewerProps) {
  return (
    <div className="rounded-lg border border-gray-200 overflow-hidden divide-y divide-gray-100">
      {changes.map((change, i) => (
        <div key={i}>
          <div className="bg-gray-50 px-3 py-1.5 flex items-center gap-2">
            <span className="text-xs font-mono font-semibold text-gray-600">{change.fieldName}</span>
          </div>
          <div className="grid grid-cols-2 divide-x divide-gray-100">
            <div className="bg-red-50 px-3 py-2">
              <p className="text-2xs font-bold text-red-400 mb-1">BEFORE</p>
              <code className="text-xs text-red-700 break-all block">
                {change.oldValue != null ? JSON.stringify(change.oldValue) : <span className="text-gray-400 italic">null</span>}
              </code>
            </div>
            <div className="bg-green-50 px-3 py-2">
              <p className="text-2xs font-bold text-green-400 mb-1">AFTER</p>
              <code className="text-xs text-green-700 break-all block">
                {change.newValue != null ? JSON.stringify(change.newValue) : <span className="text-gray-400 italic">null</span>}
              </code>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
