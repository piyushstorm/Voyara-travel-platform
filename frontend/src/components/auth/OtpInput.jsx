import { useState, useRef, useCallback, useEffect } from 'react'

export default function OtpInput({ length = 6, value, onChange, disabled, error }) {
  const [digits, setDigits] = useState(Array(length).fill(''))
  const inputRefs = useRef([])

  // Sync external value changes
  useEffect(() => {
    if (value !== undefined && value !== null) {
      const newDigits = value.toString().split('').slice(0, length)
      while (newDigits.length < length) newDigits.push('')
      setDigits(newDigits)
    }
  }, [value, length])

  const handleChange = useCallback((index, val) => {
    if (disabled) return

    // Handle paste (multiple characters)
    if (val.length > 1) {
      const pasted = val.replace(/\D/g, '').slice(0, length).split('')
      const newDigits = [...digits]
      pasted.forEach((d, i) => {
        if (index + i < length) newDigits[index + i] = d
      })
      setDigits(newDigits)
      onChange(newDigits.join(''))
      // Focus last filled or next empty
      const nextIndex = Math.min(index + pasted.length, length - 1)
      inputRefs.current[nextIndex]?.focus()
      return
    }

    // Handle single digit
    const digit = val.replace(/\D/g, '')
    const newDigits = [...digits]
    newDigits[index] = digit
    setDigits(newDigits)
    onChange(newDigits.join(''))

    // Auto-advance to next input
    if (digit && index < length - 1) {
      inputRefs.current[index + 1]?.focus()
    }
  }, [digits, disabled, length, onChange])

  const handleKeyDown = useCallback((index, e) => {
    if (disabled) return

    if (e.key === 'Backspace') {
      e.preventDefault()
      const newDigits = [...digits]
      if (digits[index]) {
        // Clear current digit
        newDigits[index] = ''
        setDigits(newDigits)
        onChange(newDigits.join(''))
      } else if (index > 0) {
        // Move to previous and clear
        newDigits[index - 1] = ''
        setDigits(newDigits)
        onChange(newDigits.join(''))
        inputRefs.current[index - 1]?.focus()
      }
    } else if (e.key === 'ArrowLeft' && index > 0) {
      inputRefs.current[index - 1]?.focus()
    } else if (e.key === 'ArrowRight' && index < length - 1) {
      inputRefs.current[index + 1]?.focus()
    }
  }, [digits, disabled, length, onChange])

  const handlePaste = useCallback((e) => {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length)
    if (pasted) {
      const newDigits = pasted.split('')
      while (newDigits.length < length) newDigits.push('')
      setDigits(newDigits)
      onChange(newDigits.join(''))
      inputRefs.current[Math.min(pasted.length, length - 1)]?.focus()
    }
  }, [length, onChange])

  return (
    <div className="flex gap-2 sm:gap-3 justify-center">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={el => inputRefs.current[index] = el}
          type="tel"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={length}
          value={digit}
          onChange={e => handleChange(index, e.target.value)}
          onKeyDown={e => handleKeyDown(index, e)}
          onPaste={handlePaste}
          disabled={disabled}
          aria-label={`Digit ${index + 1} of ${length}`}
          className={`w-10 h-12 sm:w-12 sm:h-14 text-center text-xl font-bold rounded-xl border-2 outline-none transition-all
            ${digit ? 'border-primary bg-primary/5 text-gray-900' : 'border-gray-200 bg-white text-gray-900'}
            ${error ? 'border-red-300 bg-red-50' : ''}
            focus:border-primary focus:ring-2 focus:ring-primary/20
            disabled:opacity-50 disabled:bg-gray-50`}
        />
      ))}
    </div>
  )
}
