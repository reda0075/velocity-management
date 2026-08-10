import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../sidebar/sidebar';
import { Topbar } from '../topbar/topbar';
import { ToastContainer } from '../../ui/toast-container/toast-container';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, Sidebar, Topbar, ToastContainer],
  templateUrl: './shell.html',
  styleUrl: './shell.scss'
})
export class Shell {}