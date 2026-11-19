import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.min.css';
import '@fontsource-variable/inter';
import './estilos/tema.css';
import './estilos/layout.css';
import './estilos/componentes.css';
import 'bootstrap';
import { iniciarRoteador } from './roteador.js';

iniciarRoteador(document.getElementById('app'));
