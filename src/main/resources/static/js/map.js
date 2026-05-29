/**
 * initialises the leaflet.js map centred on thessaloniki.
 * uses cartodb positron light tiles and custom svg lightning-bolt markers.
 *
 * exports:
 *   initMap(onMarkerClick) - creates the map, loads stations and places markers.
 *   highlightMarker(stationId) - pans to and opens the popup for a given station.
 */

let mapInstance = null;
const markerMap  = {};   // stationId → Leaflet marker
const busyMap    = {};   // stationId → boolean (has bookings today)
let activeMarkerId = null;

function initMap(onMarkerClick) {
  if (mapInstance) return mapInstance;

  mapInstance = L.map('map', { zoomControl: true }).setView([40.6401, 22.9444], 13);

  // cartodb positron tiles - clean and light, matches the apps colour scheme
  L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> © <a href="https://carto.com/attributions">CartoDB</a>',
    subdomains: 'abcd',
    maxZoom: 19
  }).addTo(mapInstance);

  API.get('/api/stations').then(stations => {
    stations.forEach(station => {
      const busy = station.todayBookingCount > 0;
      busyMap[station.id] = busy;
      const icon = createMarkerIcon(false, busy);

      const marker = L.marker([station.latitude, station.longitude], { icon })
        .addTo(mapInstance)
        .bindPopup(buildPopupHtml(station), {
          maxWidth: 240,
          className: 'volt-popup'
        });

      markerMap[station.id] = marker;

      marker.on('click', () => {
        setActiveMarker(station.id);
        if (typeof onMarkerClick === 'function') onMarkerClick(station);
      });
    });
  });

  return mapInstance;
}

function createMarkerIcon(selected, busy = false) {
  const bg = selected ? '#00C48C' : '#3B6FFF';
  const busyBadge = (!selected && busy)
    ? `<span class="marker-busy-dot" title="Has bookings today"></span>`
    : '';
  return L.divIcon({
    className: 'custom-marker',
    html: `<div class="marker-pin${selected ? ' selected-marker' : ''}" style="background:${bg};">
             <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
               <path d="M13 2L4.09 12.11A1 1 0 0 0 5 14H11L11 22L19.91 11.89A1 1 0 0 0 19 10H13L13 2Z"
                     fill="white" stroke="white" stroke-width="1" stroke-linejoin="round"/>
             </svg>
             ${busyBadge}
           </div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 18],
    popupAnchor: [0, -20]
  });
}

function buildPopupHtml(station) {
  const busyTag = station.todayBookingCount > 0
    ? `<span class="popup-busy-tag">🔶 ${station.todayBookingCount} booking${station.todayBookingCount > 1 ? 's' : ''} today</span>`
    : `<span class="popup-free-tag">🟢 Available today</span>`;
  return `<div class="map-popup">
    <div class="map-popup__name">${escHtml(station.name)}</div>
    <div class="map-popup__addr">📍 ${escHtml(station.address)}</div>
    <div class="map-popup__addr" style="margin-bottom:6px; color:var(--text-muted);">${station.connectorCount} connector(s)</div>
    <div style="margin-bottom:10px;">${busyTag}</div>
    <button class="map-popup__btn" onclick="onMapStationClick(${station.id})">View Details →</button>
  </div>`;
}

function setActiveMarker(stationId) {
  if (activeMarkerId && markerMap[activeMarkerId]) {
    markerMap[activeMarkerId].setIcon(createMarkerIcon(false, busyMap[activeMarkerId] || false));
  }
  if (markerMap[stationId]) {
    markerMap[stationId].setIcon(createMarkerIcon(true, false));
    activeMarkerId = stationId;
  }
}

function highlightMarker(stationId) {
  const marker = markerMap[stationId];
  if (marker) {
    setActiveMarker(stationId);
    mapInstance.panTo(marker.getLatLng(), { animate: true, duration: 0.5 });
    marker.openPopup();
  }
}

// called by the "view details" button inside the leaflet popup, wich is rendered as raw html so it needs a global function
function onMapStationClick(stationId) {
  if (typeof handleStationSelect === 'function') {
    handleStationSelect(stationId);
  }
}

function escHtml(str) {
  return String(str ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
