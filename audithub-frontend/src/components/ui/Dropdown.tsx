import { Fragment } from 'react';
import { Menu, Transition } from '@headlessui/react';
import { cn } from '../../utils/formatting.utils';

interface DropdownItem {
  label:     string;
  onClick?:  () => void;
  icon?:     React.ReactNode;
  danger?:   boolean;
  disabled?: boolean;
  divider?:  boolean;
}

interface DropdownProps {
  trigger:  React.ReactNode;
  items:    DropdownItem[];
  align?:   'left' | 'right';
}

export function Dropdown({ trigger, items, align = 'right' }: DropdownProps) {
  return (
    <Menu as="div" className="relative inline-block text-left">
      <Menu.Button as={Fragment}>{trigger}</Menu.Button>
      <Transition
        enter="transition duration-100 ease-out"
        enterFrom="opacity-0 scale-95" enterTo="opacity-100 scale-100"
        leave="transition duration-75 ease-in"
        leaveFrom="opacity-100 scale-100" leaveTo="opacity-0 scale-95"
      >
        <Menu.Items
          className={cn(
            'absolute z-50 mt-1 w-48 rounded-lg bg-white shadow-panel border border-gray-100',
            'focus:outline-none divide-y divide-gray-50',
            align === 'right' ? 'right-0' : 'left-0'
          )}
        >
          {items.map((item, i) =>
            item.divider ? (
              <div key={i} className="h-px bg-gray-100 my-1" />
            ) : (
              <Menu.Item key={i} disabled={item.disabled}>
                {({ active }) => (
                  <button
                    onClick={item.onClick}
                    className={cn(
                      'flex w-full items-center gap-2 px-3 py-2 text-sm transition-colors text-left cursor-pointer',
                      active && !item.danger && 'bg-gray-50 text-gray-900',
                      active && item.danger  && 'bg-red-50 text-red-700',
                      !active && item.danger && 'text-red-600',
                      !active && !item.danger && 'text-gray-700',
                      item.disabled && 'opacity-50 cursor-not-allowed'
                    )}
                  >
                    {item.icon && <span className="h-4 w-4 flex-shrink-0">{item.icon}</span>}
                    {item.label}
                  </button>
                )}
              </Menu.Item>
            )
          )}
        </Menu.Items>
      </Transition>
    </Menu>
  );
}
