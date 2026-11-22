/**
 * Compact "What-If" Live Parameter Slider Bar rendered directly above the oscilloscope
 * bezel. Dragging updates the preview smoothly; releasing commits the parameter patch
 * and triggers an automatic simulation re-run when Auto-Run is enabled.
 */
import { useEffect, useMemo, useState } from 'react';
import type { EditorComponent } from '../model/types';
import { formatEngineeringValue, parseEngineeringValue } from '../model/componentSchema';
import { listSweepableParameters } from './parametricSweep';
import {
  MAX_PINNED_SLIDERS,
  sliderPositionToValue,
  valueToSliderPosition,
  type PinnedSlider,
} from './liveSliders';

export interface LiveSliderBarProps {
  pinnedSliders: PinnedSlider[];
  components: EditorComponent[];
  isOpen: boolean;
  onPinSlider: (componentName: string, paramKey: string) => void;
  onUnpinSlider: (id: string) => void;
  onUpdateSliderConfig: (
    id: string,
    patch: Partial<Pick<PinnedSlider, 'min' | 'max' | 'scale'>>,
  ) => void;
  onCommitValue: (
    componentName: string,
    paramKey: string,
    value: number,
    autoRun: boolean,
  ) => void | Promise<void>;
  isSimRunning?: boolean;
}

export function LiveSliderBar({
  pinnedSliders,
  components,
  isOpen,
  onPinSlider,
  onUnpinSlider,
  onUpdateSliderConfig,
  onCommitValue,
  isSimRunning = false,
}: LiveSliderBarProps) {
  const [autoRun, setAutoRun] = useState(true);
  const [draftValues, setDraftValues] = useState<Record<string, number>>({});
  const [textInputs, setTextInputs] = useState<Record<string, string>>({});
  const [configOpenId, setConfigOpenId] = useState<string | null>(null);
  const [minDraft, setMinDraft] = useState('');
  const [maxDraft, setMaxDraft] = useState('');
  const [scaleDraft, setScaleDraft] = useState<'linear' | 'log'>('linear');

  const sweepable = useMemo(() => listSweepableParameters(components), [components]);
  const pinnedIdSet = useMemo(() => new Set(pinnedSliders.map((s) => s.id)), [pinnedSliders]);
  const availableToPin = useMemo(
    () => sweepable.filter((p) => !pinnedIdSet.has(p.parameterPath)),
    [sweepable, pinnedIdSet],
  );

  // Resolve current live value from components map
  const liveValues = useMemo(() => {
    const out: Record<string, number> = {};
    for (const s of pinnedSliders) {
      const comp = components.find((c) => c.name === s.componentName);
      const raw = comp?.parameters?.[s.paramKey];
      const num = typeof raw === 'number' ? raw : Number(raw);
      out[s.id] = Number.isFinite(num) ? num : s.initialValue;
    }
    return out;
  }, [pinnedSliders, components]);

  // Clear stale draft when live component value syncs back
  useEffect(() => {
    setDraftValues({});
    setTextInputs({});
  }, [liveValues]);

  if (!isOpen && pinnedSliders.length === 0) {
    return null;
  }

  const openConfigPopover = (s: PinnedSlider) => {
    if (configOpenId === s.id) {
      setConfigOpenId(null);
      return;
    }
    setMinDraft(formatEngineeringValue(s.min));
    setMaxDraft(formatEngineeringValue(s.max));
    setScaleDraft(s.scale);
    setConfigOpenId(s.id);
  };

  const applyConfigPopover = (s: PinnedSlider) => {
    const pMin = parseEngineeringValue(minDraft);
    const pMax = parseEngineeringValue(maxDraft);
    if (pMin === null || pMax === null || pMax <= pMin) return;
    if (scaleDraft === 'log' && (pMin <= 0 || pMax <= 0)) return;
    onUpdateSliderConfig(s.id, { min: pMin, max: pMax, scale: scaleDraft });
    setConfigOpenId(null);
  };

  return (
    <div className="live-slider-bar" data-testid="live-slider-bar">
      <div className="live-slider-bar-header">
        <div className="live-slider-header-left">
          <span className="live-slider-title">🎚 Live Parameter Tuner</span>
          <select
            className="live-slider-add-select"
            data-testid="live-slider-add-select"
            value=""
            disabled={pinnedSliders.length >= MAX_PINNED_SLIDERS || availableToPin.length === 0}
            onChange={(e) => {
              const val = e.target.value;
              if (!val) return;
              const match = availableToPin.find((p) => p.parameterPath === val);
              if (match) {
                onPinSlider(match.componentName, match.paramKey);
              }
            }}
            title={
              pinnedSliders.length >= MAX_PINNED_SLIDERS
                ? `Maximum of ${MAX_PINNED_SLIDERS} pinned sliders reached`
                : 'Pin a component parameter to tune live'
            }
          >
            <option value="">
              {pinnedSliders.length >= MAX_PINNED_SLIDERS
                ? `Max ${MAX_PINNED_SLIDERS} sliders pinned`
                : '+ Pin Parameter...'}
            </option>
            {availableToPin.map((p) => (
              <option key={p.parameterPath} value={p.parameterPath}>
                {p.componentName} · {p.label} ({formatEngineeringValue(p.currentValue, p.unit)})
              </option>
            ))}
          </select>
        </div>

        <label className="live-slider-autorun">
          <input
            type="checkbox"
            data-testid="live-sliders-autorun"
            checked={autoRun}
            onChange={(e) => setAutoRun(e.target.checked)}
          />
          <span>Auto-Run on release</span>
          {isSimRunning && <span className="live-slider-running-dot" title="Simulation running..." />}
        </label>
      </div>

      {pinnedSliders.length === 0 ? (
        <div className="live-slider-empty">
          Pick a component parameter above to tune it with a live slider.
        </div>
      ) : (
        <div className="live-slider-cards">
          {pinnedSliders.map((slider) => {
            const currentVal = draftValues[slider.id] ?? liveValues[slider.id] ?? slider.initialValue;
            const sliderPos = valueToSliderPosition(currentVal, slider.min, slider.max, slider.scale);
            const displayInput =
              textInputs[slider.id] !== undefined
                ? textInputs[slider.id]
                : formatEngineeringValue(currentVal);

            const commitDraft = (val: number) => {
              void onCommitValue(slider.componentName, slider.paramKey, val, autoRun);
            };

            const handleTextCommit = () => {
              const raw = textInputs[slider.id];
              if (raw === undefined) return;
              const parsed = parseEngineeringValue(raw);
              setTextInputs((prev) => {
                const copy = { ...prev };
                delete copy[slider.id];
                return copy;
              });
              if (parsed !== null && Number.isFinite(parsed)) {
                commitDraft(parsed);
              }
            };

            return (
              <div key={slider.id} className="live-slider-card" data-testid={`live-slider-${slider.id}`}>
                <div className="live-slider-card-top">
                  <span className="live-slider-card-label" title={`${slider.componentName}.${slider.label}`}>
                    {slider.componentName} · {slider.label}
                  </span>
                  <div className="live-slider-card-actions">
                    <button
                      type="button"
                      className="live-slider-icon-btn"
                      data-testid={`slider-reset-${slider.id}`}
                      title={`Reset to initial (${formatEngineeringValue(slider.initialValue, slider.unit)})`}
                      onClick={() => commitDraft(slider.initialValue)}
                    >
                      ↺
                    </button>
                    <button
                      type="button"
                      className={`live-slider-icon-btn ${configOpenId === slider.id ? 'active' : ''}`}
                      data-testid={`slider-config-${slider.id}`}
                      title="Configure slider bounds & scale"
                      onClick={() => openConfigPopover(slider)}
                    >
                      ⚙
                    </button>
                    <button
                      type="button"
                      className="live-slider-icon-btn"
                      data-testid={`slider-unpin-${slider.id}`}
                      title="Unpin slider"
                      onClick={() => onUnpinSlider(slider.id)}
                    >
                      ✕
                    </button>
                  </div>
                </div>

                <div className="live-slider-control-row">
                  <input
                    type="range"
                    min={0}
                    max={1000}
                    value={sliderPos}
                    data-testid={`slider-range-${slider.id}`}
                    className="live-slider-range"
                    onChange={(e) => {
                      const nextVal = sliderPositionToValue(
                        Number(e.target.value),
                        slider.min,
                        slider.max,
                        slider.scale,
                      );
                      setDraftValues((prev) => ({ ...prev, [slider.id]: nextVal }));
                    }}
                    onPointerUp={() => {
                      const drafted = draftValues[slider.id];
                      if (drafted !== undefined) {
                        commitDraft(drafted);
                      }
                    }}
                    onTouchEnd={() => {
                      const drafted = draftValues[slider.id];
                      if (drafted !== undefined) {
                        commitDraft(drafted);
                      }
                    }}
                    onKeyUp={(e) => {
                      if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'].includes(e.key)) {
                        const drafted = draftValues[slider.id];
                        if (drafted !== undefined) {
                          commitDraft(drafted);
                        }
                      }
                    }}
                  />
                  <div className="live-slider-val-wrap">
                    <input
                      type="text"
                      className="live-slider-val-input"
                      data-testid={`slider-input-${slider.id}`}
                      value={displayInput}
                      onChange={(e) => setTextInputs((prev) => ({ ...prev, [slider.id]: e.target.value }))}
                      onBlur={handleTextCommit}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          handleTextCommit();
                        } else if (e.key === 'Escape') {
                          setTextInputs((prev) => {
                            const copy = { ...prev };
                            delete copy[slider.id];
                            return copy;
                          });
                        }
                      }}
                    />
                    {slider.unit && <span className="live-slider-unit">{slider.unit}</span>}
                  </div>
                </div>

                {configOpenId === slider.id && (
                  <div className="live-slider-config-popover" data-testid={`slider-popover-${slider.id}`}>
                    <label>
                      <span>Min</span>
                      <input type="text" value={minDraft} onChange={(e) => setMinDraft(e.target.value)} />
                    </label>
                    <label>
                      <span>Max</span>
                      <input type="text" value={maxDraft} onChange={(e) => setMaxDraft(e.target.value)} />
                    </label>
                    <label>
                      <span>Scale</span>
                      <select
                        value={scaleDraft}
                        onChange={(e) => setScaleDraft(e.target.value as 'linear' | 'log')}
                      >
                        <option value="linear">Linear</option>
                        <option value="log">Log</option>
                      </select>
                    </label>
                    <button
                      type="button"
                      className="sim-mini-btn active"
                      onClick={() => applyConfigPopover(slider)}
                    >
                      Save
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
