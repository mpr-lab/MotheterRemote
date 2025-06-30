#!/bin/bash

BOOT_DIR="/boot"

echo "=== Raspberry Pi Mode Configuration ==="
echo "Will this Pi use radio communication? (y/n)"
read -r USE_RADIO

# Clean up any existing flags
rm -f "$BOOT_DIR"/is_*.flag

if [[ "$USE_RADIO" == "y" || "$USE_RADIO" == "Y" ]]; then
  echo "Is this the MAIN Pi or an ACCESSORY Pi?"
  echo "Type 'main' or 'accessory':"
  read -r WHICH_PI

  if [[ "$WHICH_PI" == "main" ]]; then
    sudo touch "$BOOT_DIR/is_main_radio.flag"
    echo "Set mode: MAIN radio Pi"
  elif [[ "$WHICH_PI" == "accessory" ]]; then
    sudo touch "$BOOT_DIR/is_accessory_radio.flag"
    echo "Set mode: ACCESSORY radio Pi"
  else
    echo "Invalid input. No flag created."
  fi
else
  echo "Set mode: NON-radio Pi (default)"
fi

echo "Flags set. You can now reboot or continue setup."
