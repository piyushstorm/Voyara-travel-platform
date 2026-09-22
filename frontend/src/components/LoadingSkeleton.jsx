export function FlightCardSkeleton() {
  return (
    <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="skeleton w-10 h-10 rounded-full" />
          <div>
            <div className="skeleton w-24 h-4 mb-2" />
            <div className="skeleton w-16 h-3" />
          </div>
        </div>
        <div className="flex-1 mx-8">
          <div className="skeleton w-full h-3 mb-2" />
          <div className="skeleton w-20 h-3 mx-auto" />
        </div>
        <div className="text-right">
          <div className="skeleton w-20 h-6 mb-2 ml-auto" />
          <div className="skeleton w-16 h-3 ml-auto" />
        </div>
      </div>
    </div>
  )
}

export function HotelCardSkeleton() {
  return (
    <div className="bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100">
      <div className="flex flex-col sm:flex-row">
        <div className="skeleton w-full sm:w-48 h-40 sm:h-auto" />
        <div className="flex-1 p-4">
          <div className="skeleton w-40 h-5 mb-2" />
          <div className="skeleton w-24 h-3 mb-3" />
          <div className="skeleton w-full h-3 mb-2" />
          <div className="skeleton w-3/4 h-3 mb-4" />
          <div className="skeleton w-28 h-6" />
        </div>
      </div>
    </div>
  )
}

export function SearchSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      {[1, 2, 3, 4].map((i) => (
        <div key={i}>
          <div className="skeleton w-16 h-3 mb-2" />
          <div className="skeleton w-full h-12 rounded-lg" />
        </div>
      ))}
    </div>
  )
}
