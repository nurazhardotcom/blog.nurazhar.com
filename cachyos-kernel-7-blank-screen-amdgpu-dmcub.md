Title: CachyOS Kernel 7.0.12 Blank Screen — AMD Barcelo DMCUB Firmware Crash
Date: 2026-06-22
Tags: cachyos, amd, amdgpu, kernel, dmcub, troubleshooting
Description: Kernel 7.0.12 blank screen on AMD Barcelo GPUs — DMCUB firmware mismatch. Fix with amdgpu.dc=0, and whether the mainline kernel is worth the boot hassle over LTS.

---

After upgrading to `linux-cachyos 7.0.12-1`, my laptop boots to a blank screen. Not a kernel panic — the system starts, services load, but the display never initialises.

This is a known AMDGPU regression with the **DMCUB (Display Micro Controller) firmware** on certain APUs. Here's the diagnosis and the practical question: is the mainline kernel worth the trouble?

---

## The Failure

My GPU is an AMD Barcelo APU (`1002:15e7`, Ryzen 5000 series with Vega graphics). On kernel 7.0.12, the journal shows:

```
Jun 22 06:28:52 cachyos kernel: amdgpu 0000:03:00.0: [drm] Loading DMUB firmware via PSP: version=0x0101002B
Jun 22 06:29:00 cachyos kernel: amdgpu 0000:03:00.0: [drm] *ERROR* dc_dmub_srv_log_diagnostic_data: DMCUB error
Jun 22 06:29:01 cachyos kernel: amdgpu 0000:03:00.0: [drm] *ERROR* dpcd_set_link_settings: core_link_write_dpcd failed
```

The DMCUB firmware (version `0x0101002B`, loaded via the Platform Security Processor) is too old for the kernel 7.x amdgpu driver. The display engine initialises, finds an incompatible firmware interface, and silently fails — blank screen.

The LTS kernel (`linux-cachyos-lts 6.18.35-1`) boots fine because its amdgpu driver expects the older firmware interface.

---

## The Fix

At the Limine boot menu, highlight the latest kernel entry and press `e` to edit the command line. Add:

```
amdgpu.dc=0
```

This disables the new **Display Core (DC)** and falls back to the legacy display driver. Trade-offs:

- ✅ Boots correctly
- ❌ No DisplayPort Multi-Stream Transport (MST)
- ❌ No HDMI/DP audio via amdgpu
- ❌ Potentially lower power efficiency on displays

For a permanent fix, add it to the kernel cmdline permanently:

```bash
# Make amdgpu.dc=0 permanent
sudo sed -i 's/root=UUID=[^ ]*/& amdgpu.dc=0/' /boot/limine.conf
```

Or just boot the LTS kernel until the next kernel update which may carry a newer DMCUB firmware interface.

---

## Can You Fix It Without Rebooting?

No. Kernel command-line parameters are parsed by the kernel at boot time, before userspace starts. You cannot set `amdgpu.dc=0` dynamically — it's a module parameter that controls early GPU initialisation.

You could try unloading and reloading the amdgpu module:

```bash
sudo modprobe -r amdgpu && sudo modprobe amdgpu dc=0
```

But this kills your display session. You'd need to do it from a TTY (Ctrl+Alt+F2), which defeats the purpose if you can't get to a TTY because the display is blank.

The only practical approach is a one-time boot with `amdgpu.dc=0` added at the Limine menu, then making it permanent if it works.

---

## Is the Mainline Kernel Worth It?

Here's what you get on `linux-cachyos 7.0.12-1` vs `linux-cachyos-lts 6.18.35-1`:

| Feature | Mainline (7.0.12) | LTS (6.18.35) |
|---------|------------------|---------------|
| Scheduler | EEVDF + Cachy Sauce (BORE) | EEVDF + Cachy Sauce |
| LTO | ✅ Full LTO | ❌ |
| AutoFDO | ✅ Profile-guided | ❌ |
| Propeller | ✅ Propeller PGO | ❌ |
| Sched-ext | ✅ | ✅ |
| Newer drivers | ✅ Latest amdgpu, etc. | ❌ Backported fixes |
| Stability | ⚠️ May have regressions | ✅ Battle-tested |

The main kernel has **LTO (Link Time Optimization)**, **AutoFDO**, and **Propeller** — all profile-guided optimizations that the compiler uses to reorder hot code paths for better cache locality and branch prediction. In practice:

- **CPU-bound workloads**: ~5-15% faster (compilation, encoding, scientific computing)
- **Desktop responsiveness**: BORE scheduler gives snappier interactive feel
- **Gaming**: Better frametimes, lower 1% lows
- **Daily browsing/dev**: Imperceptible difference

For a developer who spends most of their time in a browser, terminal, and editor, the LTS kernel is perfectly adequate. The main kernel matters if you:

1. Compile code frequently
2. Run CPU-bound batch jobs
3. Game on the machine
4. Need the very latest hardware support (newer WiFi, GPU, NVMe)

---

## Bottom Line

If the blank screen is annoying and you don't need the main kernel's optimisations, **run the LTS kernel**:

```bash
# In /boot/limine.conf
default_entry: 2        # Points to linux-cachyos-lts
remember_last_entry: no  # Don't change after manual selection
```

The LTS kernel is the upstream kernel.org LTS branch with CachyOS's sauce on top. It's stable, well-tested, and your GPU works. The main kernel's LTO/AutoFDO/Propeller gains are real but marginal for everyday desktop use.

If you want the main kernel, add `amdgpu.dc=0` and verify it works before making it permanent. Report the DMCUB firmware regression to the CachyOS issue tracker — a future kernel update may ship with a compatible firmware interface.

---

*Hardware: AMD Barcelo APU (1002:15e7). CachyOS April 2026 release. linux-cachyos 7.0.12-1 / linux-cachyos-lts 6.18.35-1. Firmware: linux-firmware 20260519-1.*
